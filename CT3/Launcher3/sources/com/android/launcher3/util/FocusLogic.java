package com.android.launcher3.util;

import android.view.View;
import android.view.ViewGroup;
import com.android.launcher3.CellLayout;
import com.android.launcher3.ShortcutAndWidgetContainer;
import java.lang.reflect.Array;
import java.util.Arrays;

public class FocusLogic {
    public static boolean shouldConsume(int keyCode) {
        return keyCode == 21 || keyCode == 22 || keyCode == 19 || keyCode == 20 || keyCode == 122 || keyCode == 123 || keyCode == 92 || keyCode == 93 || keyCode == 67 || keyCode == 112;
    }

    public static int handleKeyEvent(int keyCode, int[][] map, int iconIdx, int pageIndex, int pageCount, boolean isRtl) {
        int cntX = map == null ? -1 : map.length;
        int cntY = map == null ? -1 : map[0].length;
        switch (keyCode) {
            case 19:
                int newIndex = handleDpadVertical(iconIdx, cntX, cntY, map, -1);
                return newIndex;
            case 20:
                int newIndex2 = handleDpadVertical(iconIdx, cntX, cntY, map, 1);
                return newIndex2;
            case 21:
                int newIndex3 = handleDpadHorizontal(iconIdx, cntX, cntY, map, -1, isRtl);
                if (!isRtl && newIndex3 == -1 && pageIndex > 0) {
                    return -2;
                }
                if (isRtl && newIndex3 == -1 && pageIndex < pageCount - 1) {
                    return -10;
                }
                return newIndex3;
            case 22:
                int newIndex4 = handleDpadHorizontal(iconIdx, cntX, cntY, map, 1, isRtl);
                if (!isRtl && newIndex4 == -1 && pageIndex < pageCount - 1) {
                    return -9;
                }
                if (isRtl && newIndex4 == -1 && pageIndex > 0) {
                    return -5;
                }
                return newIndex4;
            case 92:
                int newIndex5 = handlePageUp(pageIndex);
                return newIndex5;
            case 93:
                int newIndex6 = handlePageDown(pageIndex, pageCount);
                return newIndex6;
            case 122:
                int newIndex7 = handleMoveHome();
                return newIndex7;
            case 123:
                int newIndex8 = handleMoveEnd();
                return newIndex8;
            default:
                return -1;
        }
    }

    private static int[][] createFullMatrix(int m, int n) {
        int[][] matrix = (int[][]) Array.newInstance((Class<?>) Integer.TYPE, m, n);
        for (int i = 0; i < m; i++) {
            Arrays.fill(matrix[i], -1);
        }
        return matrix;
    }

    public static int[][] createSparseMatrix(CellLayout layout) {
        ShortcutAndWidgetContainer parent = layout.getShortcutsAndWidgets();
        int m = layout.getCountX();
        int n = layout.getCountY();
        boolean invert = parent.invertLayoutHorizontally();
        int[][] matrix = createFullMatrix(m, n);
        for (int i = 0; i < parent.getChildCount(); i++) {
            View cell = parent.getChildAt(i);
            if (cell.isFocusable()) {
                int cx = ((CellLayout.LayoutParams) cell.getLayoutParams()).cellX;
                int cy = ((CellLayout.LayoutParams) cell.getLayoutParams()).cellY;
                if (invert) {
                    cx = (m - cx) - 1;
                }
                matrix[cx][cy] = i;
            }
        }
        return matrix;
    }

    public static int[][] createSparseMatrixWithHotseat(CellLayout iconLayout, CellLayout hotseatLayout, boolean isHotseatHorizontal, int allappsiconRank) {
        boolean moreIconsInHotseatThanWorkspace;
        int m;
        int n;
        ViewGroup iconParent = iconLayout.getShortcutsAndWidgets();
        ViewGroup hotseatParent = hotseatLayout.getShortcutsAndWidgets();
        if (isHotseatHorizontal) {
            moreIconsInHotseatThanWorkspace = hotseatLayout.getCountX() > iconLayout.getCountX();
        } else {
            moreIconsInHotseatThanWorkspace = hotseatLayout.getCountY() > iconLayout.getCountY();
        }
        if (isHotseatHorizontal) {
            m = hotseatLayout.getCountX();
            n = iconLayout.getCountY() + hotseatLayout.getCountY();
        } else {
            m = iconLayout.getCountX() + hotseatLayout.getCountX();
            n = hotseatLayout.getCountY();
        }
        int[][] matrix = createFullMatrix(m, n);
        if (moreIconsInHotseatThanWorkspace) {
            if (isHotseatHorizontal) {
                for (int j = 0; j < n; j++) {
                    matrix[allappsiconRank][j] = -11;
                }
            } else {
                for (int j2 = 0; j2 < m; j2++) {
                    matrix[j2][allappsiconRank] = -11;
                }
            }
        }
        for (int i = 0; i < iconParent.getChildCount(); i++) {
            View cell = iconParent.getChildAt(i);
            if (cell.isFocusable()) {
                int cx = ((CellLayout.LayoutParams) cell.getLayoutParams()).cellX;
                int cy = ((CellLayout.LayoutParams) cell.getLayoutParams()).cellY;
                if (moreIconsInHotseatThanWorkspace) {
                    if (isHotseatHorizontal && cx >= allappsiconRank) {
                        cx++;
                    }
                    if (!isHotseatHorizontal && cy >= allappsiconRank) {
                        cy++;
                    }
                }
                matrix[cx][cy] = i;
            }
        }
        for (int i2 = hotseatParent.getChildCount() - 1; i2 >= 0; i2--) {
            if (isHotseatHorizontal) {
                matrix[((CellLayout.LayoutParams) hotseatParent.getChildAt(i2).getLayoutParams()).cellX][iconLayout.getCountY()] = iconParent.getChildCount() + i2;
            } else {
                matrix[iconLayout.getCountX()][((CellLayout.LayoutParams) hotseatParent.getChildAt(i2).getLayoutParams()).cellY] = iconParent.getChildCount() + i2;
            }
        }
        return matrix;
    }

    public static int[][] createSparseMatrixWithPivotColumn(CellLayout iconLayout, int pivotX, int pivotY) {
        ViewGroup iconParent = iconLayout.getShortcutsAndWidgets();
        int[][] matrix = createFullMatrix(iconLayout.getCountX() + 1, iconLayout.getCountY());
        for (int i = 0; i < iconParent.getChildCount(); i++) {
            View cell = iconParent.getChildAt(i);
            if (cell.isFocusable()) {
                int cx = ((CellLayout.LayoutParams) cell.getLayoutParams()).cellX;
                int cy = ((CellLayout.LayoutParams) cell.getLayoutParams()).cellY;
                if (pivotX < 0) {
                    matrix[cx - pivotX][cy] = i;
                } else {
                    matrix[cx][cy] = i;
                }
            }
        }
        if (pivotX < 0) {
            matrix[0][pivotY] = 100;
        } else {
            matrix[pivotX][pivotY] = 100;
        }
        return matrix;
    }

    private static int handleDpadHorizontal(int iconIdx, int cntX, int cntY, int[][] matrix, int increment, boolean isRtl) {
        if (matrix == null) {
            throw new IllegalStateException("Dpad navigation requires a matrix.");
        }
        int newIconIndex = -1;
        int xPos = -1;
        int yPos = -1;
        for (int i = 0; i < cntX; i++) {
            for (int j = 0; j < cntY; j++) {
                if (matrix[i][j] == iconIdx) {
                    xPos = i;
                    yPos = j;
                }
            }
        }
        int x = xPos + increment;
        while (x >= 0 && x < cntX) {
            newIconIndex = inspectMatrix(x, yPos, cntX, cntY, matrix);
            if (newIconIndex == -1 || newIconIndex == -11) {
                x += increment;
            } else {
                return newIconIndex;
            }
        }
        boolean haveCrossedAllAppsColumn1 = false;
        boolean haveCrossedAllAppsColumn2 = false;
        for (int coeff = 1; coeff < cntY; coeff++) {
            int nextYPos1 = yPos + (coeff * increment);
            int nextYPos2 = yPos - (coeff * increment);
            int x2 = xPos + (increment * coeff);
            if (inspectMatrix(x2, nextYPos1, cntX, cntY, matrix) == -11) {
                haveCrossedAllAppsColumn1 = true;
            }
            if (inspectMatrix(x2, nextYPos2, cntX, cntY, matrix) == -11) {
                haveCrossedAllAppsColumn2 = true;
            }
            while (x2 >= 0 && x2 < cntX) {
                int offset1 = (!haveCrossedAllAppsColumn1 || x2 >= cntX + (-1)) ? 0 : increment;
                int newIconIndex2 = inspectMatrix(x2, nextYPos1 + offset1, cntX, cntY, matrix);
                if (newIconIndex2 != -1) {
                    return newIconIndex2;
                }
                int offset2 = (!haveCrossedAllAppsColumn2 || x2 >= cntX + (-1)) ? 0 : -increment;
                newIconIndex = inspectMatrix(x2, nextYPos2 + offset2, cntX, cntY, matrix);
                if (newIconIndex == -1) {
                    x2 += increment;
                } else {
                    return newIconIndex;
                }
            }
        }
        if (iconIdx == 100) {
            return isRtl ? increment < 0 ? -8 : -4 : increment < 0 ? -4 : -8;
        }
        return newIconIndex;
    }

    private static int handleDpadVertical(int iconIndex, int cntX, int cntY, int[][] matrix, int increment) {
        int newIconIndex = -1;
        if (matrix == null) {
            throw new IllegalStateException("Dpad navigation requires a matrix.");
        }
        int xPos = -1;
        int yPos = -1;
        for (int i = 0; i < cntX; i++) {
            for (int j = 0; j < cntY; j++) {
                if (matrix[i][j] == iconIndex) {
                    xPos = i;
                    yPos = j;
                }
            }
        }
        int y = yPos + increment;
        while (y >= 0 && y < cntY && y >= 0) {
            newIconIndex = inspectMatrix(xPos, y, cntX, cntY, matrix);
            if (newIconIndex == -1 || newIconIndex == -11) {
                y += increment;
            } else {
                return newIconIndex;
            }
        }
        boolean haveCrossedAllAppsColumn1 = false;
        boolean haveCrossedAllAppsColumn2 = false;
        for (int coeff = 1; coeff < cntX; coeff++) {
            int nextXPos1 = xPos + (coeff * increment);
            int nextXPos2 = xPos - (coeff * increment);
            int y2 = yPos + (increment * coeff);
            if (inspectMatrix(nextXPos1, y2, cntX, cntY, matrix) == -11) {
                haveCrossedAllAppsColumn1 = true;
            }
            if (inspectMatrix(nextXPos2, y2, cntX, cntY, matrix) == -11) {
                haveCrossedAllAppsColumn2 = true;
            }
            while (y2 >= 0 && y2 < cntY) {
                int offset1 = (!haveCrossedAllAppsColumn1 || y2 >= cntY + (-1)) ? 0 : increment;
                int newIconIndex2 = inspectMatrix(nextXPos1 + offset1, y2, cntX, cntY, matrix);
                if (newIconIndex2 != -1) {
                    return newIconIndex2;
                }
                int offset2 = (!haveCrossedAllAppsColumn2 || y2 >= cntY + (-1)) ? 0 : -increment;
                newIconIndex = inspectMatrix(nextXPos2 + offset2, y2, cntX, cntY, matrix);
                if (newIconIndex == -1) {
                    y2 += increment;
                } else {
                    return newIconIndex;
                }
            }
        }
        return newIconIndex;
    }

    private static int handleMoveHome() {
        return -6;
    }

    private static int handleMoveEnd() {
        return -7;
    }

    private static int handlePageDown(int pageIndex, int pageCount) {
        if (pageIndex < pageCount - 1) {
            return -8;
        }
        return -7;
    }

    private static int handlePageUp(int pageIndex) {
        if (pageIndex > 0) {
            return -3;
        }
        return -6;
    }

    private static boolean isValid(int xPos, int yPos, int countX, int countY) {
        return xPos >= 0 && xPos < countX && yPos >= 0 && yPos < countY;
    }

    private static int inspectMatrix(int x, int y, int cntX, int cntY, int[][] matrix) {
        if (!isValid(x, y, cntX, cntY) || matrix[x][y] == -1) {
            return -1;
        }
        int newIconIndex = matrix[x][y];
        return newIconIndex;
    }

    public static View getAdjacentChildInNextFolderPage(ShortcutAndWidgetContainer nextPage, View oldView, int edgeColumn) {
        int newRow = ((CellLayout.LayoutParams) oldView.getLayoutParams()).cellY;
        for (int column = (edgeColumn == -9) ^ nextPage.invertLayoutHorizontally() ? 0 : ((CellLayout) nextPage.getParent()).getCountX() - 1; column >= 0; column--) {
            for (int row = newRow; row >= 0; row--) {
                View newView = nextPage.getChildAt(column, row);
                if (newView != null) {
                    return newView;
                }
            }
        }
        return null;
    }
}
