package com.android.documentsui.base;

import android.database.AbstractCursor;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import com.android.documentsui.DocumentsFeatureOption;

public class FilteringCursorWrapper extends AbstractCursor {
    private int mCount;
    private final Cursor mCursor;
    private int mDrmLevel;
    private final int[] mPosition;

    public FilteringCursorWrapper(Cursor cursor, String[] strArr, String[] strArr2) {
        this(cursor, strArr, strArr2, Long.MIN_VALUE);
    }

    public FilteringCursorWrapper(Cursor cursor, String[] strArr, String[] strArr2, long j) {
        this.mDrmLevel = -1;
        this.mCursor = cursor;
        int count = cursor.getCount();
        this.mPosition = new int[count];
        cursor.moveToPosition(-1);
        while (cursor.moveToNext() && this.mCount < count) {
            String cursorString = DocumentInfo.getCursorString(cursor, "mime_type");
            long cursorLong = DocumentInfo.getCursorLong(cursor, "last_modified");
            if (strArr2 == null || !MimeTypes.mimeMatches(strArr2, cursorString)) {
                if (cursorLong >= j && MimeTypes.mimeMatches(strArr, cursorString)) {
                    int[] iArr = this.mPosition;
                    int i = this.mCount;
                    this.mCount = i + 1;
                    iArr[i] = cursor.getPosition();
                }
            }
        }
        if (SharedMinimal.DEBUG && this.mCount != cursor.getCount()) {
            Log.d("Documents", "Before filtering " + cursor.getCount() + ", after " + this.mCount);
        }
    }

    public FilteringCursorWrapper(Cursor cursor, int i) {
        this.mDrmLevel = -1;
        this.mCursor = cursor;
        this.mDrmLevel = i;
        int count = cursor.getCount();
        this.mPosition = new int[count];
        int i2 = 15;
        if (DocumentsFeatureOption.IS_SUPPORT_DRM) {
            int i3 = this.mDrmLevel;
            if (i3 != 4) {
                switch (i3) {
                    case 1:
                        i2 = 1;
                        break;
                    case 2:
                        i2 = 4;
                        break;
                }
            }
        } else {
            i2 = 0;
        }
        cursor.moveToPosition(-1);
        for (int i4 = 0; i4 < count; i4++) {
            cursor.moveToNext();
            if (DocumentsFeatureOption.IS_SUPPORT_DRM) {
                boolean z = DocumentInfo.getCursorInt(cursor, "is_drm") > 0;
                int cursorInt = DocumentInfo.getCursorInt(cursor, "drm_method");
                if (!z || ((this.mDrmLevel <= 0 || cursorInt >= 0) && (i2 & cursorInt) != 0)) {
                }
            } else {
                this.mPosition[this.mCount] = cursor.getPosition();
                this.mCount++;
            }
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
    public boolean onMove(int i, int i2) {
        return this.mCursor.moveToPosition(this.mPosition[i2]);
    }

    @Override
    public String[] getColumnNames() {
        return this.mCursor.getColumnNames();
    }

    @Override
    public int getCount() {
        return this.mCount;
    }

    @Override
    public double getDouble(int i) {
        return this.mCursor.getDouble(i);
    }

    @Override
    public float getFloat(int i) {
        return this.mCursor.getFloat(i);
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
    public short getShort(int i) {
        return this.mCursor.getShort(i);
    }

    @Override
    public String getString(int i) {
        return this.mCursor.getString(i);
    }

    @Override
    public int getType(int i) {
        return this.mCursor.getType(i);
    }

    @Override
    public boolean isNull(int i) {
        return this.mCursor.isNull(i);
    }
}
