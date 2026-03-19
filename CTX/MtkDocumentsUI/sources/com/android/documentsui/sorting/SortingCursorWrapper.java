package com.android.documentsui.sorting;

import android.database.AbstractCursor;
import android.database.Cursor;
import android.os.Bundle;
import com.android.documentsui.R;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.Lookup;
import com.android.documentsui.base.Shared;

class SortingCursorWrapper extends AbstractCursor {
    private final Cursor mCursor;
    private final int[] mPosition;

    public SortingCursorWrapper(Cursor cursor, SortDimension sortDimension, Lookup<String, String> lookup) {
        long[] jArr;
        this.mCursor = cursor;
        int count = cursor.getCount();
        this.mPosition = new int[count];
        boolean[] zArr = new boolean[count];
        String[] strArr = new String[count];
        int id = sortDimension.getId();
        String[] strArr2 = null;
        if (id == 16908310) {
            jArr = null;
            strArr2 = new String[count];
        } else if (id == R.id.date) {
            jArr = new long[count];
        } else if (id != R.id.file_type) {
            if (id != R.id.size) {
                jArr = null;
            }
        }
        cursor.moveToPosition(-1);
        for (int i = 0; i < count; i++) {
            cursor.moveToNext();
            this.mPosition[i] = i;
            String cursorString = DocumentInfo.getCursorString(this.mCursor, "mime_type");
            zArr[i] = "vnd.android.document/directory".equals(cursorString);
            strArr[i] = DocumentInfo.getCursorString(this.mCursor, "document_id");
            if (id == 16908310) {
                strArr2[i] = DocumentInfo.getCursorString(this.mCursor, "_display_name");
            } else if (id == R.id.date) {
                jArr[i] = getLastModified(this.mCursor);
            } else if (id == R.id.file_type) {
                strArr2[i] = lookup.lookup(cursorString);
            } else if (id == R.id.size) {
                jArr[i] = DocumentInfo.getCursorLong(this.mCursor, "_size");
            }
        }
        if (id != 16908310) {
            if (id != R.id.date) {
                if (id != R.id.file_type) {
                    if (id != R.id.size) {
                        return;
                    }
                }
            }
            binarySort(jArr, zArr, this.mPosition, strArr, sortDimension.getSortDirection());
            return;
        }
        binarySort(strArr2, zArr, this.mPosition, strArr, sortDimension.getSortDirection());
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
        return this.mCursor.getCount();
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

    @Override
    public Bundle getExtras() {
        return this.mCursor.getExtras();
    }

    private static long getLastModified(Cursor cursor) {
        long cursorLong = DocumentInfo.getCursorLong(cursor, "last_modified");
        if (cursorLong == -1) {
            return Long.MAX_VALUE;
        }
        return cursorLong;
    }

    private static void binarySort(String[] strArr, boolean[] zArr, int[] iArr, String[] strArr2, int i) {
        int iCompareToIgnoreCaseNullable;
        int length = iArr.length;
        for (int i2 = 1; i2 < length; i2++) {
            int i3 = iArr[i2];
            String str = strArr[i2];
            boolean z = zArr[i2];
            String str2 = strArr2[i2];
            int i4 = 0;
            int i5 = i2;
            while (i4 < i5) {
                int i6 = (i4 + i5) >>> 1;
                boolean z2 = zArr[i6];
                if (z && !z2) {
                    iCompareToIgnoreCaseNullable = -1;
                } else if (z || !z2) {
                    String str3 = strArr[i6];
                    switch (i) {
                        case 1:
                            iCompareToIgnoreCaseNullable = Shared.compareToIgnoreCaseNullable(str, str3);
                            break;
                        case 2:
                            iCompareToIgnoreCaseNullable = -Shared.compareToIgnoreCaseNullable(str, str3);
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown sorting direction: " + i);
                    }
                } else {
                    iCompareToIgnoreCaseNullable = 1;
                }
                if (iCompareToIgnoreCaseNullable == 0) {
                    iCompareToIgnoreCaseNullable = str2.compareTo(strArr2[i6]);
                }
                if (iCompareToIgnoreCaseNullable >= 0) {
                    i4 = i6 + 1;
                } else {
                    i5 = i6;
                }
            }
            int i7 = i2 - i4;
            switch (i7) {
                case 1:
                    break;
                case 2:
                    int i8 = i4 + 2;
                    int i9 = i4 + 1;
                    iArr[i8] = iArr[i9];
                    strArr[i8] = strArr[i9];
                    zArr[i8] = zArr[i9];
                    break;
                default:
                    int i10 = i4 + 1;
                    System.arraycopy(iArr, i4, iArr, i10, i7);
                    System.arraycopy(strArr, i4, strArr, i10, i7);
                    System.arraycopy(zArr, i4, zArr, i10, i7);
                    continue;
                    iArr[i4] = i3;
                    strArr[i4] = str;
                    zArr[i4] = z;
                    break;
            }
            int i11 = i4 + 1;
            iArr[i11] = iArr[i4];
            strArr[i11] = strArr[i4];
            zArr[i11] = zArr[i4];
            iArr[i4] = i3;
            strArr[i4] = str;
            zArr[i4] = z;
        }
    }

    private static void binarySort(long[] jArr, boolean[] zArr, int[] iArr, String[] strArr, int i) {
        int i2;
        int length = iArr.length;
        int iCompare = 1;
        int i3 = 1;
        while (i3 < length) {
            int i4 = iArr[i3];
            long j = jArr[i3];
            boolean z = zArr[i3];
            String str = strArr[i3];
            int i5 = 0;
            int i6 = i3;
            while (i5 < i6) {
                int i7 = (i5 + i6) >>> iCompare;
                boolean z2 = zArr[i7];
                if (!z || z2) {
                    if (z || !z2) {
                        i2 = i3;
                        long j2 = jArr[i7];
                        switch (i) {
                            case 1:
                                iCompare = Long.compare(j, j2);
                                break;
                            case 2:
                                iCompare = -Long.compare(j, j2);
                                break;
                            default:
                                throw new IllegalArgumentException("Unknown sorting direction: " + i);
                        }
                    } else {
                        i2 = i3;
                    }
                } else {
                    i2 = i3;
                    iCompare = -1;
                }
                if (iCompare == 0) {
                    iCompare = str.compareTo(strArr[i7]);
                }
                if (iCompare >= 0) {
                    i5 = i7 + 1;
                } else {
                    i6 = i7;
                }
                i3 = i2;
                iCompare = 1;
            }
            int i8 = i3;
            int i9 = i8 - i5;
            switch (i9) {
                case 1:
                    break;
                case 2:
                    int i10 = i5 + 2;
                    int i11 = i5 + 1;
                    iArr[i10] = iArr[i11];
                    jArr[i10] = jArr[i11];
                    zArr[i10] = zArr[i11];
                    strArr[i10] = strArr[i11];
                    break;
                default:
                    int i12 = i5 + 1;
                    System.arraycopy(iArr, i5, iArr, i12, i9);
                    System.arraycopy(jArr, i5, jArr, i12, i9);
                    System.arraycopy(zArr, i5, zArr, i12, i9);
                    System.arraycopy(strArr, i5, strArr, i12, i9);
                    continue;
                    iArr[i5] = i4;
                    jArr[i5] = j;
                    zArr[i5] = z;
                    strArr[i5] = str;
                    i3 = i8 + 1;
                    iCompare = 1;
                    break;
            }
            int i13 = i5 + 1;
            iArr[i13] = iArr[i5];
            jArr[i13] = jArr[i5];
            zArr[i13] = zArr[i5];
            strArr[i13] = strArr[i5];
            iArr[i5] = i4;
            jArr[i5] = j;
            zArr[i5] = z;
            strArr[i5] = str;
            i3 = i8 + 1;
            iCompare = 1;
        }
    }
}
