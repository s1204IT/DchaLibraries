package com.android.documentsui;

import android.database.AbstractCursor;
import android.database.Cursor;
import android.os.Bundle;
import com.android.documentsui.model.DocumentInfo;

public class SortingCursorWrapper extends AbstractCursor {
    private final Cursor mCursor;
    private final int[] mPosition;
    private final long[] mValueLong;
    private final String[] mValueString;

    public SortingCursorWrapper(Cursor cursor, int sortOrder) {
        this.mCursor = cursor;
        int count = cursor.getCount();
        this.mPosition = new int[count];
        switch (sortOrder) {
            case 1:
                this.mValueString = new String[count];
                this.mValueLong = null;
                break;
            case 2:
            case 3:
                this.mValueString = null;
                this.mValueLong = new long[count];
                break;
            default:
                throw new IllegalArgumentException();
        }
        cursor.moveToPosition(-1);
        for (int i = 0; i < count; i++) {
            cursor.moveToNext();
            this.mPosition[i] = i;
            switch (sortOrder) {
                case 1:
                    String mimeType = DocumentInfo.getCursorString(cursor, "mime_type");
                    String displayName = DocumentInfo.getCursorString(cursor, "_display_name");
                    if ("vnd.android.document/directory".equals(mimeType)) {
                        this.mValueString[i] = (char) 1 + displayName;
                    } else {
                        this.mValueString[i] = displayName;
                    }
                    break;
                case 2:
                    this.mValueLong[i] = DocumentInfo.getCursorLong(cursor, "last_modified");
                    break;
                case 3:
                    this.mValueLong[i] = DocumentInfo.getCursorLong(cursor, "_size");
                    break;
            }
        }
        switch (sortOrder) {
            case 1:
                synchronized (SortingCursorWrapper.class) {
                    binarySort(this.mPosition, this.mValueString);
                    break;
                }
                return;
            case 2:
            case 3:
                binarySort(this.mPosition, this.mValueLong);
                return;
            default:
                return;
        }
    }

    @Override
    public Bundle getExtras() {
        return this.mCursor.getExtras();
    }

    @Override
    public void close() {
        super.close();
        this.mCursor.close();
    }

    @Override
    public boolean onMove(int oldPosition, int newPosition) {
        return this.mCursor.moveToPosition(this.mPosition[newPosition]);
    }

    @Override
    public String[] getColumnNames() {
        return this.mCursor.getColumnNames();
    }

    @Override
    public int getCount() {
        return this.mCursor.getCount();
    }

    @Override
    public double getDouble(int column) {
        return this.mCursor.getDouble(column);
    }

    @Override
    public float getFloat(int column) {
        return this.mCursor.getFloat(column);
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
    public short getShort(int column) {
        return this.mCursor.getShort(column);
    }

    @Override
    public String getString(int column) {
        return this.mCursor.getString(column);
    }

    @Override
    public int getType(int column) {
        return this.mCursor.getType(column);
    }

    @Override
    public boolean isNull(int column) {
        return this.mCursor.isNull(column);
    }

    private static void binarySort(int[] position, String[] value) {
        int count = position.length;
        for (int start = 1; start < count; start++) {
            int pivotPosition = position[start];
            String pivotValue = value[start];
            int left = 0;
            int right = start;
            while (left < right) {
                int mid = (left + right) >>> 1;
                String rhs = value[mid];
                int compare = DocumentInfo.compareToIgnoreCaseNullable(pivotValue, rhs);
                if (compare < 0) {
                    right = mid;
                } else {
                    left = mid + 1;
                }
            }
            int n = start - left;
            switch (n) {
                case 1:
                    break;
                case 2:
                    position[left + 2] = position[left + 1];
                    value[left + 2] = value[left + 1];
                    break;
                default:
                    System.arraycopy(position, left, position, left + 1, n);
                    System.arraycopy(value, left, value, left + 1, n);
                    continue;
                    position[left] = pivotPosition;
                    value[left] = pivotValue;
                    break;
            }
            position[left + 1] = position[left];
            value[left + 1] = value[left];
            position[left] = pivotPosition;
            value[left] = pivotValue;
        }
    }

    private static void binarySort(int[] position, long[] value) {
        int count = position.length;
        for (int start = 1; start < count; start++) {
            int pivotPosition = position[start];
            long pivotValue = value[start];
            int left = 0;
            int right = start;
            while (left < right) {
                int mid = (left + right) >>> 1;
                long rhs = value[mid];
                int compare = Long.compare(pivotValue, rhs);
                if (compare > 0) {
                    right = mid;
                } else {
                    left = mid + 1;
                }
            }
            int n = start - left;
            switch (n) {
                case 1:
                    break;
                case 2:
                    position[left + 2] = position[left + 1];
                    value[left + 2] = value[left + 1];
                    break;
                default:
                    System.arraycopy(position, left, position, left + 1, n);
                    System.arraycopy(value, left, value, left + 1, n);
                    continue;
                    position[left] = pivotPosition;
                    value[left] = pivotValue;
                    break;
            }
            position[left + 1] = position[left];
            value[left + 1] = value[left];
            position[left] = pivotPosition;
            value[left] = pivotValue;
        }
    }
}
