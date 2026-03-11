package com.android.launcher3.util;

import android.util.Log;
import android.view.View;
import com.android.launcher3.CellLayout;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.ShortcutAndWidgetContainer;
import java.lang.reflect.Array;
import java.util.Arrays;

public class FocusLogic {
    public static final int ALL_APPS_COLUMN = -11;
    public static final int CURRENT_PAGE_FIRST_ITEM = -6;
    public static final int CURRENT_PAGE_LAST_ITEM = -7;
    private static final boolean DEBUG = false;
    public static final int EMPTY = -1;
    public static final int NEXT_PAGE_FIRST_ITEM = -8;
    public static final int NEXT_PAGE_LEFT_COLUMN = -9;
    public static final int NEXT_PAGE_RIGHT_COLUMN = -10;
    public static final int NOOP = -1;
    public static final int PIVOT = 100;
    public static final int PREVIOUS_PAGE_FIRST_ITEM = -3;
    public static final int PREVIOUS_PAGE_LAST_ITEM = -4;
    public static final int PREVIOUS_PAGE_LEFT_COLUMN = -5;
    public static final int PREVIOUS_PAGE_RIGHT_COLUMN = -2;
    private static final String TAG = "FocusLogic";

    public static boolean shouldConsume(int i) {
        return i == 21 || i == 22 || i == 19 || i == 20 || i == 122 || i == 123 || i == 92 || i == 93;
    }

    public static int handleKeyEvent(int i, int[][] iArr, int i2, int i3, int i4, boolean z) {
        int length;
        int length2;
        int iHandleDpadHorizontal;
        if (iArr != null) {
            length = iArr.length;
        } else {
            length = -1;
        }
        if (iArr != null) {
            length2 = iArr[0].length;
        } else {
            length2 = -1;
        }
        switch (i) {
            case 19:
                return handleDpadVertical(i2, length, length2, iArr, -1);
            case 20:
                return handleDpadVertical(i2, length, length2, iArr, 1);
            case 21:
                iHandleDpadHorizontal = handleDpadHorizontal(i2, length, length2, iArr, -1, z);
                if (!z && iHandleDpadHorizontal == -1 && i3 > 0) {
                    return -2;
                }
                if (z && iHandleDpadHorizontal == -1 && i3 < i4 - 1) {
                    return -10;
                }
                break;
            case 22:
                iHandleDpadHorizontal = handleDpadHorizontal(i2, length, length2, iArr, 1, z);
                if (!z && iHandleDpadHorizontal == -1 && i3 < i4 - 1) {
                    return -9;
                }
                if (z && iHandleDpadHorizontal == -1 && i3 > 0) {
                    return -5;
                }
                break;
            case 92:
                return handlePageUp(i3);
            case 93:
                return handlePageDown(i3, i4);
            case 122:
                return handleMoveHome();
            case 123:
                return handleMoveEnd();
            default:
                return -1;
        }
        return iHandleDpadHorizontal;
    }

    private static int[][] createFullMatrix(int i, int i2) {
        int[][] iArr = (int[][]) Array.newInstance((Class<?>) int.class, i, i2);
        for (int i3 = 0; i3 < i; i3++) {
            Arrays.fill(iArr[i3], -1);
        }
        return iArr;
    }

    public static int[][] createSparseMatrix(CellLayout cellLayout) {
        ShortcutAndWidgetContainer shortcutsAndWidgets = cellLayout.getShortcutsAndWidgets();
        int countX = cellLayout.getCountX();
        int countY = cellLayout.getCountY();
        boolean zInvertLayoutHorizontally = shortcutsAndWidgets.invertLayoutHorizontally();
        int[][] iArrCreateFullMatrix = createFullMatrix(countX, countY);
        for (int i = 0; i < shortcutsAndWidgets.getChildCount(); i++) {
            View childAt = shortcutsAndWidgets.getChildAt(i);
            if (childAt.isFocusable()) {
                int i2 = ((CellLayout.LayoutParams) childAt.getLayoutParams()).cellX;
                int i3 = ((CellLayout.LayoutParams) childAt.getLayoutParams()).cellY;
                if (zInvertLayoutHorizontally) {
                    i2 = (countX - i2) - 1;
                }
                if (i2 < countX && i3 < countY) {
                    iArrCreateFullMatrix[i2][i3] = i;
                }
            }
        }
        return iArrCreateFullMatrix;
    }

    public static int[][] createSparseMatrixWithHotseat(CellLayout cellLayout, CellLayout cellLayout2, DeviceProfile deviceProfile) {
        int countX;
        int countY;
        ShortcutAndWidgetContainer shortcutsAndWidgets = cellLayout.getShortcutsAndWidgets();
        ShortcutAndWidgetContainer shortcutsAndWidgets2 = cellLayout2.getShortcutsAndWidgets();
        boolean z = !deviceProfile.isVerticalBarLayout();
        if (z) {
            countX = cellLayout2.getCountX();
            countY = cellLayout.getCountY() + cellLayout2.getCountY();
        } else {
            countX = cellLayout.getCountX() + cellLayout2.getCountX();
            countY = cellLayout2.getCountY();
        }
        int[][] iArrCreateFullMatrix = createFullMatrix(countX, countY);
        for (int i = 0; i < shortcutsAndWidgets.getChildCount(); i++) {
            View childAt = shortcutsAndWidgets.getChildAt(i);
            if (childAt.isFocusable()) {
                iArrCreateFullMatrix[((CellLayout.LayoutParams) childAt.getLayoutParams()).cellX][((CellLayout.LayoutParams) childAt.getLayoutParams()).cellY] = i;
            }
        }
        for (int childCount = shortcutsAndWidgets2.getChildCount() - 1; childCount >= 0; childCount--) {
            if (z) {
                iArrCreateFullMatrix[((CellLayout.LayoutParams) shortcutsAndWidgets2.getChildAt(childCount).getLayoutParams()).cellX][cellLayout.getCountY()] = shortcutsAndWidgets.getChildCount() + childCount;
            } else {
                iArrCreateFullMatrix[cellLayout.getCountX()][((CellLayout.LayoutParams) shortcutsAndWidgets2.getChildAt(childCount).getLayoutParams()).cellY] = shortcutsAndWidgets.getChildCount() + childCount;
            }
        }
        return iArrCreateFullMatrix;
    }

    public static int[][] createSparseMatrixWithPivotColumn(CellLayout cellLayout, int i, int i2) {
        ShortcutAndWidgetContainer shortcutsAndWidgets = cellLayout.getShortcutsAndWidgets();
        int[][] iArrCreateFullMatrix = createFullMatrix(cellLayout.getCountX() + 1, cellLayout.getCountY());
        for (int i3 = 0; i3 < shortcutsAndWidgets.getChildCount(); i3++) {
            View childAt = shortcutsAndWidgets.getChildAt(i3);
            if (childAt.isFocusable()) {
                int i4 = ((CellLayout.LayoutParams) childAt.getLayoutParams()).cellX;
                int i5 = ((CellLayout.LayoutParams) childAt.getLayoutParams()).cellY;
                if (i < 0) {
                    iArrCreateFullMatrix[i4 - i][i5] = i3;
                } else {
                    iArrCreateFullMatrix[i4][i5] = i3;
                }
            }
        }
        if (i < 0) {
            iArrCreateFullMatrix[0][i2] = 100;
        } else {
            iArrCreateFullMatrix[i][i2] = 100;
        }
        return iArrCreateFullMatrix;
    }

    private static int handleDpadHorizontal(int i, int i2, int i3, int[][] iArr, int i4, boolean z) {
        if (iArr == null) {
            throw new IllegalStateException("Dpad navigation requires a matrix.");
        }
        int i5 = -1;
        int i6 = -1;
        int i7 = 0;
        while (i7 < i2) {
            int i8 = i6;
            int i9 = i5;
            for (int i10 = 0; i10 < i3; i10++) {
                if (iArr[i7][i10] == i) {
                    i9 = i7;
                    i8 = i10;
                }
            }
            i7++;
            i5 = i9;
            i6 = i8;
        }
        int i11 = i5 + i4;
        int iInspectMatrix = -1;
        while (i11 >= 0 && i11 < i2) {
            iInspectMatrix = inspectMatrix(i11, i6, i2, i3, iArr);
            if (iInspectMatrix == -1 || iInspectMatrix == -11) {
                i11 += i4;
            } else {
                return iInspectMatrix;
            }
        }
        int iInspectMatrix2 = iInspectMatrix;
        boolean z2 = false;
        boolean z3 = false;
        for (int i12 = 1; i12 < i3; i12++) {
            int i13 = i12 * i4;
            int i14 = i6 + i13;
            int i15 = i6 - i13;
            int i16 = i13 + i5;
            if (inspectMatrix(i16, i14, i2, i3, iArr) == -11) {
                z2 = true;
            }
            if (inspectMatrix(i16, i15, i2, i3, iArr) == -11) {
                z3 = true;
            }
            while (i16 >= 0 && i16 < i2) {
                int iInspectMatrix3 = inspectMatrix(i16, ((!z2 || i16 >= i2 + (-1)) ? 0 : i4) + i14, i2, i3, iArr);
                if (iInspectMatrix3 != -1) {
                    return iInspectMatrix3;
                }
                iInspectMatrix2 = inspectMatrix(i16, ((!z3 || i16 >= i2 + (-1)) ? 0 : -i4) + i15, i2, i3, iArr);
                if (iInspectMatrix2 == -1) {
                    i16 += i4;
                } else {
                    return iInspectMatrix2;
                }
            }
        }
        if (i == 100) {
            return z ? i4 < 0 ? -8 : -4 : i4 < 0 ? -4 : -8;
        }
        return iInspectMatrix2;
    }

    private static int handleDpadVertical(int i, int i2, int i3, int[][] iArr, int i4) {
        int i5;
        if (iArr == null) {
            throw new IllegalStateException("Dpad navigation requires a matrix.");
        }
        int i6 = -1;
        int i7 = -1;
        int i8 = 0;
        while (i8 < i2) {
            int i9 = i7;
            int i10 = i6;
            for (int i11 = 0; i11 < i3; i11++) {
                if (iArr[i8][i11] == i) {
                    i9 = i8;
                    i10 = i11;
                }
            }
            i8++;
            i6 = i10;
            i7 = i9;
        }
        int i12 = i6 + i4;
        int iInspectMatrix = -1;
        while (i12 >= 0 && i12 < i3 && i12 >= 0) {
            iInspectMatrix = inspectMatrix(i7, i12, i2, i3, iArr);
            if (iInspectMatrix == -1 || iInspectMatrix == -11) {
                i12 += i4;
            } else {
                return iInspectMatrix;
            }
        }
        int iInspectMatrix2 = iInspectMatrix;
        boolean z = false;
        boolean z2 = false;
        for (int i13 = 1; i13 < i2; i13++) {
            int i14 = i13 * i4;
            int i15 = i7 + i14;
            int i16 = i7 - i14;
            int i17 = i14 + i6;
            if (inspectMatrix(i15, i17, i2, i3, iArr) == -11) {
                z = true;
            }
            if (inspectMatrix(i16, i17, i2, i3, iArr) == -11) {
                z2 = true;
            }
            while (i17 >= 0 && i17 < i3) {
                int iInspectMatrix3 = inspectMatrix(((!z || i17 >= i3 + (-1)) ? 0 : i4) + i15, i17, i2, i3, iArr);
                if (iInspectMatrix3 != -1) {
                    return iInspectMatrix3;
                }
                if (z2 && i17 < i3 - 1) {
                    i5 = -i4;
                } else {
                    i5 = 0;
                }
                iInspectMatrix2 = inspectMatrix(i5 + i16, i17, i2, i3, iArr);
                if (iInspectMatrix2 == -1) {
                    i17 += i4;
                } else {
                    return iInspectMatrix2;
                }
            }
        }
        return iInspectMatrix2;
    }

    private static int handleMoveHome() {
        return -6;
    }

    private static int handleMoveEnd() {
        return -7;
    }

    private static int handlePageDown(int i, int i2) {
        if (i < i2 - 1) {
            return -8;
        }
        return -7;
    }

    private static int handlePageUp(int i) {
        if (i > 0) {
            return -3;
        }
        return -6;
    }

    private static boolean isValid(int i, int i2, int i3, int i4) {
        return i >= 0 && i < i3 && i2 >= 0 && i2 < i4;
    }

    private static int inspectMatrix(int i, int i2, int i3, int i4, int[][] iArr) {
        if (!isValid(i, i2, i3, i4) || iArr[i][i2] == -1) {
            return -1;
        }
        return iArr[i][i2];
    }

    private static String getStringIndex(int i) {
        switch (i) {
            case ALL_APPS_COLUMN:
                return "ALL_APPS_COLUMN";
            case NEXT_PAGE_RIGHT_COLUMN:
            case PREVIOUS_PAGE_LEFT_COLUMN:
            default:
                return Integer.toString(i);
            case NEXT_PAGE_LEFT_COLUMN:
                return "NEXT_PAGE_LEFT_COLUMN";
            case NEXT_PAGE_FIRST_ITEM:
                return "NEXT_PAGE_FIRST";
            case CURRENT_PAGE_LAST_ITEM:
                return "CURRENT_PAGE_LAST";
            case CURRENT_PAGE_FIRST_ITEM:
                return "CURRENT_PAGE_FIRST";
            case -4:
                return "PREVIOUS_PAGE_LAST";
            case -3:
                return "PREVIOUS_PAGE_FIRST";
            case -2:
                return "PREVIOUS_PAGE_RIGHT_COLUMN";
            case -1:
                return "NOOP";
        }
    }

    private static void printMatrix(int[][] iArr) {
        Log.v(TAG, "\tprintMap:");
        int length = iArr[0].length;
        for (int i = 0; i < length; i++) {
            String str = "\t\t";
            for (int[] iArr2 : iArr) {
                str = str + String.format("%3d", Integer.valueOf(iArr2[i]));
            }
            Log.v(TAG, str);
        }
    }

    public static View getAdjacentChildInNextFolderPage(ShortcutAndWidgetContainer shortcutAndWidgetContainer, View view, int i) {
        int i2 = ((CellLayout.LayoutParams) view.getLayoutParams()).cellY;
        for (int countX = (i == -9) ^ shortcutAndWidgetContainer.invertLayoutHorizontally() ? 0 : ((CellLayout) shortcutAndWidgetContainer.getParent()).getCountX() - 1; countX >= 0; countX--) {
            for (int i3 = i2; i3 >= 0; i3--) {
                View childAt = shortcutAndWidgetContainer.getChildAt(countX, i3);
                if (childAt != null) {
                    return childAt;
                }
            }
        }
        return null;
    }
}
