package com.android.internal.database;

import android.database.AbstractCursor;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.util.Log;
import java.lang.reflect.Array;

public class SortCursor extends AbstractCursor {
    private static final String TAG = "SortCursor";
    private int[][] mCurRowNumCache;
    private Cursor mCursor;
    private Cursor[] mCursors;
    private int[] mSortColumns;
    private final int ROWCACHESIZE = 64;
    private int[] mRowNumCache = new int[64];
    private int[] mCursorCache = new int[64];
    private int mLastCacheHit = -1;
    private DataSetObserver mObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            SortCursor.this.mPos = -1;
        }

        @Override
        public void onInvalidated() {
            SortCursor.this.mPos = -1;
        }
    };

    public SortCursor(Cursor[] cursorArr, String str) {
        this.mCursors = cursorArr;
        int length = this.mCursors.length;
        this.mSortColumns = new int[length];
        for (int i = 0; i < length; i++) {
            if (this.mCursors[i] != null) {
                this.mCursors[i].registerDataSetObserver(this.mObserver);
                this.mCursors[i].moveToFirst();
                this.mSortColumns[i] = this.mCursors[i].getColumnIndexOrThrow(str);
            }
        }
        this.mCursor = null;
        String str2 = "";
        for (int i2 = 0; i2 < length; i2++) {
            if (this.mCursors[i2] != null && !this.mCursors[i2].isAfterLast()) {
                String string = this.mCursors[i2].getString(this.mSortColumns[i2]);
                if (this.mCursor == null || string.compareToIgnoreCase(str2) < 0) {
                    this.mCursor = this.mCursors[i2];
                    str2 = string;
                }
            }
        }
        for (int length2 = this.mRowNumCache.length - 1; length2 >= 0; length2--) {
            this.mRowNumCache[length2] = -2;
        }
        this.mCurRowNumCache = (int[][]) Array.newInstance((Class<?>) int.class, 64, length);
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
        if (i == i2) {
            return true;
        }
        int i3 = i2 % 64;
        if (this.mRowNumCache[i3] == i2) {
            int i4 = this.mCursorCache[i3];
            this.mCursor = this.mCursors[i4];
            if (this.mCursor == null) {
                Log.w(TAG, "onMove: cache results in a null cursor.");
                return false;
            }
            this.mCursor.moveToPosition(this.mCurRowNumCache[i3][i4]);
            this.mLastCacheHit = i3;
            return true;
        }
        this.mCursor = null;
        int length = this.mCursors.length;
        if (this.mLastCacheHit >= 0) {
            for (int i5 = 0; i5 < length; i5++) {
                if (this.mCursors[i5] != null) {
                    this.mCursors[i5].moveToPosition(this.mCurRowNumCache[this.mLastCacheHit][i5]);
                }
            }
        }
        if (i2 < i || i == -1) {
            for (int i6 = 0; i6 < length; i6++) {
                if (this.mCursors[i6] != null) {
                    this.mCursors[i6].moveToFirst();
                }
            }
            i = 0;
        }
        if (i < 0) {
            i = 0;
        }
        int i7 = -1;
        while (true) {
            if (i > i2) {
                break;
            }
            int i8 = -1;
            String str = "";
            for (int i9 = 0; i9 < length; i9++) {
                if (this.mCursors[i9] != null && !this.mCursors[i9].isAfterLast()) {
                    String string = this.mCursors[i9].getString(this.mSortColumns[i9]);
                    if (i8 < 0 || string.compareToIgnoreCase(str) < 0) {
                        i8 = i9;
                        str = string;
                    }
                }
            }
            if (i != i2) {
                if (this.mCursors[i8] != null) {
                    this.mCursors[i8].moveToNext();
                }
                i++;
                i7 = i8;
            } else {
                i7 = i8;
                break;
            }
        }
        this.mCursor = this.mCursors[i7];
        this.mRowNumCache[i3] = i2;
        this.mCursorCache[i3] = i7;
        for (int i10 = 0; i10 < length; i10++) {
            if (this.mCursors[i10] != null) {
                this.mCurRowNumCache[i3][i10] = this.mCursors[i10].getPosition();
            }
        }
        this.mLastCacheHit = -1;
        return true;
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
        int length = this.mCursors.length;
        for (int i = 0; i < length; i++) {
            if (this.mCursors[i] != null) {
                return this.mCursors[i].getColumnNames();
            }
        }
        throw new IllegalStateException("No cursor that can return names");
    }

    @Override
    public void deactivate() {
        int length = this.mCursors.length;
        for (int i = 0; i < length; i++) {
            if (this.mCursors[i] != null) {
                this.mCursors[i].deactivate();
            }
        }
    }

    @Override
    public void close() {
        int length = this.mCursors.length;
        for (int i = 0; i < length; i++) {
            if (this.mCursors[i] != null) {
                this.mCursors[i].close();
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
