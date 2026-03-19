package com.android.internal.policy;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.Size;
import android.view.Gravity;
import com.android.internal.R;
import java.io.PrintWriter;
import java.util.ArrayList;

public class PipSnapAlgorithm {
    private static final float CORNER_MAGNET_THRESHOLD = 0.3f;
    private static final int SNAP_MODE_CORNERS_AND_SIDES = 1;
    private static final int SNAP_MODE_CORNERS_ONLY = 0;
    private static final int SNAP_MODE_EDGE = 2;
    private static final int SNAP_MODE_EDGE_MAGNET_CORNERS = 3;
    private static final int SNAP_MODE_LONG_EDGE_MAGNET_CORNERS = 4;
    private final Context mContext;
    private final float mDefaultSizePercent;
    private final int mFlingDeceleration;
    private boolean mIsMinimized;
    private final float mMaxAspectRatioForMinSize;
    private final float mMinAspectRatioForMinSize;
    private final int mMinimizedVisibleSize;
    private final ArrayList<Integer> mSnapGravities = new ArrayList<>();
    private final int mDefaultSnapMode = 3;
    private int mSnapMode = 3;
    private int mOrientation = 0;

    public PipSnapAlgorithm(Context context) {
        Resources resources = context.getResources();
        this.mContext = context;
        this.mMinimizedVisibleSize = resources.getDimensionPixelSize(R.dimen.pip_minimized_visible_size);
        this.mDefaultSizePercent = resources.getFloat(R.dimen.config_pictureInPictureDefaultSizePercent);
        this.mMaxAspectRatioForMinSize = resources.getFloat(R.dimen.config_pictureInPictureAspectRatioLimitForMinSize);
        this.mMinAspectRatioForMinSize = 1.0f / this.mMaxAspectRatioForMinSize;
        this.mFlingDeceleration = this.mContext.getResources().getDimensionPixelSize(R.dimen.pip_fling_deceleration);
        onConfigurationChanged();
    }

    public void onConfigurationChanged() {
        Resources resources = this.mContext.getResources();
        this.mOrientation = resources.getConfiguration().orientation;
        this.mSnapMode = resources.getInteger(R.integer.config_pictureInPictureSnapMode);
        calculateSnapTargets();
    }

    public void setMinimized(boolean z) {
        this.mIsMinimized = z;
    }

    public Rect findClosestSnapBounds(Rect rect, Rect rect2, float f, float f2, Point point) {
        Rect rect3 = new Rect(rect2);
        Point edgeIntersect = getEdgeIntersect(rect2, rect, f, f2, point);
        rect3.offsetTo(edgeIntersect.x, edgeIntersect.y);
        return findClosestSnapBounds(rect, rect3);
    }

    public Point getEdgeIntersect(Rect rect, Rect rect2, float f, float f2, Point point) {
        int i;
        boolean z = this.mOrientation == 2;
        int i2 = rect.left;
        float f3 = f2 / f;
        float f4 = rect.top - (i2 * f3);
        Point point2 = new Point();
        Point point3 = new Point();
        point2.x = f > 0.0f ? rect2.right : rect2.left;
        point2.y = findY(f3, f4, point2.x);
        point3.y = f2 > 0.0f ? rect2.bottom : rect2.top;
        point3.x = findX(f3, f4, point3.y);
        if (z) {
            if (f > 0.0f) {
                i = rect2.right - rect.left;
            } else {
                i = rect.left - rect2.left;
            }
        } else if (f2 > 0.0f) {
            i = rect2.bottom - rect.top;
        } else {
            i = rect.top - rect2.top;
        }
        if (i > 0) {
            int i3 = z ? point.y : point.x;
            int i4 = z ? point3.y : point3.x;
            int iCenterX = rect2.centerX();
            if ((i3 < iCenterX && i4 < iCenterX) || (i3 > iCenterX && i4 > iCenterX)) {
                int iMin = Math.min(((int) (0.0d - Math.pow(z ? f : f2, 2.0d))) / (this.mFlingDeceleration * 2), i);
                if (z) {
                    int i5 = rect.left;
                    if (f <= 0.0f) {
                        iMin = -iMin;
                    }
                    point3.x = i5 + iMin;
                } else {
                    int i6 = rect.top;
                    if (f2 <= 0.0f) {
                        iMin = -iMin;
                    }
                    point3.y = i6 + iMin;
                }
                return point3;
            }
        }
        double dHypot = Math.hypot(point2.x - i2, point2.y - r9);
        double dHypot2 = Math.hypot(point3.x - i2, point3.y - r9);
        if (dHypot == 0.0d) {
            return point3;
        }
        return (dHypot2 != 0.0d && Math.abs(dHypot) > Math.abs(dHypot2)) ? point3 : point2;
    }

    private int findY(float f, float f2, float f3) {
        return (int) ((f * f3) + f2);
    }

    private int findX(float f, float f2, float f3) {
        return (int) ((f3 - f2) / f);
    }

    public Rect findClosestSnapBounds(Rect rect, Rect rect2) {
        Rect rect3 = new Rect(rect.left, rect.top, rect.right + rect2.width(), rect.bottom + rect2.height());
        Rect rect4 = new Rect(rect2);
        if (this.mSnapMode == 4 || this.mSnapMode == 3) {
            Rect rect5 = new Rect();
            Point[] pointArr = new Point[this.mSnapGravities.size()];
            for (int i = 0; i < this.mSnapGravities.size(); i++) {
                Gravity.apply(this.mSnapGravities.get(i).intValue(), rect2.width(), rect2.height(), rect3, 0, 0, rect5);
                pointArr[i] = new Point(rect5.left, rect5.top);
            }
            Point pointFindClosestPoint = findClosestPoint(rect2.left, rect2.top, pointArr);
            if (distanceToPoint(pointFindClosestPoint, rect2.left, rect2.top) < Math.max(rect2.width(), rect2.height()) * CORNER_MAGNET_THRESHOLD) {
                rect4.offsetTo(pointFindClosestPoint.x, pointFindClosestPoint.y);
            } else {
                snapRectToClosestEdge(rect2, rect, rect4);
            }
        } else if (this.mSnapMode == 2) {
            snapRectToClosestEdge(rect2, rect, rect4);
        } else {
            Rect rect6 = new Rect();
            Point[] pointArr2 = new Point[this.mSnapGravities.size()];
            for (int i2 = 0; i2 < this.mSnapGravities.size(); i2++) {
                Gravity.apply(this.mSnapGravities.get(i2).intValue(), rect2.width(), rect2.height(), rect3, 0, 0, rect6);
                pointArr2[i2] = new Point(rect6.left, rect6.top);
            }
            Point pointFindClosestPoint2 = findClosestPoint(rect2.left, rect2.top, pointArr2);
            rect4.offsetTo(pointFindClosestPoint2.x, pointFindClosestPoint2.y);
        }
        return rect4;
    }

    public void applyMinimizedOffset(Rect rect, Rect rect2, Point point, Rect rect3) {
        if (rect.left <= rect2.centerX()) {
            rect.offsetTo((rect3.left + this.mMinimizedVisibleSize) - rect.width(), rect.top);
        } else {
            rect.offsetTo((point.x - rect3.right) - this.mMinimizedVisibleSize, rect.top);
        }
    }

    public float getSnapFraction(Rect rect, Rect rect2) {
        Rect rect3 = new Rect();
        snapRectToClosestEdge(rect, rect2, rect3);
        float fWidth = (rect3.left - rect2.left) / rect2.width();
        float fHeight = (rect3.top - rect2.top) / rect2.height();
        if (rect3.top == rect2.top) {
            return fWidth;
        }
        if (rect3.left == rect2.right) {
            return 1.0f + fHeight;
        }
        if (rect3.top == rect2.bottom) {
            return 2.0f + (1.0f - fWidth);
        }
        return 3.0f + (1.0f - fHeight);
    }

    public void applySnapFraction(Rect rect, Rect rect2, float f) {
        if (f < 1.0f) {
            rect.offsetTo(rect2.left + ((int) (f * rect2.width())), rect2.top);
            return;
        }
        if (f < 2.0f) {
            rect.offsetTo(rect2.right, rect2.top + ((int) ((f - 1.0f) * rect2.height())));
        } else if (f >= 3.0f) {
            rect.offsetTo(rect2.left, rect2.top + ((int) ((1.0f - (f - 3.0f)) * rect2.height())));
        } else {
            rect.offsetTo(rect2.left + ((int) ((1.0f - (f - 2.0f)) * rect2.width())), rect2.bottom);
        }
    }

    public void getMovementBounds(Rect rect, Rect rect2, Rect rect3, int i) {
        rect3.set(rect2);
        rect3.right = Math.max(rect2.left, rect2.right - rect.width());
        rect3.bottom = Math.max(rect2.top, rect2.bottom - rect.height());
        rect3.bottom -= i;
    }

    public Size getSizeForAspectRatio(float f, float f2, int i, int i2) {
        int iRound;
        int iMax = (int) Math.max(f2, Math.min(i, i2) * this.mDefaultSizePercent);
        if (f > this.mMinAspectRatioForMinSize && f <= this.mMaxAspectRatioForMinSize) {
            float f3 = iMax;
            float length = PointF.length(this.mMaxAspectRatioForMinSize * f3, f3);
            iMax = (int) Math.round(Math.sqrt((length * length) / ((f * f) + 1.0f)));
            iRound = Math.round(iMax * f);
        } else if (f <= 1.0f) {
            iMax = Math.round(iMax / f);
            iRound = iMax;
        } else {
            iRound = Math.round(iMax * f);
        }
        return new Size(iRound, iMax);
    }

    private Point findClosestPoint(int i, int i2, Point[] pointArr) {
        Point point = null;
        float f = Float.MAX_VALUE;
        for (Point point2 : pointArr) {
            float fDistanceToPoint = distanceToPoint(point2, i, i2);
            if (fDistanceToPoint < f) {
                point = point2;
                f = fDistanceToPoint;
            }
        }
        return point;
    }

    private void snapRectToClosestEdge(Rect rect, Rect rect2, Rect rect3) {
        int iMin;
        int iMax = Math.max(rect2.left, Math.min(rect2.right, rect.left));
        int iMax2 = Math.max(rect2.top, Math.min(rect2.bottom, rect.top));
        rect3.set(rect);
        if (this.mIsMinimized) {
            rect3.offsetTo(iMax, iMax2);
            return;
        }
        int iAbs = Math.abs(rect.left - rect2.left);
        int iAbs2 = Math.abs(rect.top - rect2.top);
        int iAbs3 = Math.abs(rect2.right - rect.left);
        int iAbs4 = Math.abs(rect2.bottom - rect.top);
        if (this.mSnapMode == 4) {
            if (this.mOrientation == 2) {
                iMin = Math.min(iAbs2, iAbs4);
            } else {
                iMin = Math.min(iAbs, iAbs3);
            }
        } else {
            iMin = Math.min(Math.min(iAbs, iAbs3), Math.min(iAbs2, iAbs4));
        }
        if (iMin == iAbs) {
            rect3.offsetTo(rect2.left, iMax2);
            return;
        }
        if (iMin == iAbs2) {
            rect3.offsetTo(iMax, rect2.top);
        } else if (iMin == iAbs3) {
            rect3.offsetTo(rect2.right, iMax2);
        } else {
            rect3.offsetTo(iMax, rect2.bottom);
        }
    }

    private float distanceToPoint(Point point, int i, int i2) {
        return PointF.length(point.x - i, point.y - i2);
    }

    private void calculateSnapTargets() {
        this.mSnapGravities.clear();
        switch (this.mSnapMode) {
            case 0:
            case 3:
            case 4:
                break;
            case 1:
                if (this.mOrientation == 2) {
                    this.mSnapGravities.add(49);
                    this.mSnapGravities.add(81);
                } else {
                    this.mSnapGravities.add(19);
                    this.mSnapGravities.add(21);
                }
                break;
            case 2:
            default:
                return;
        }
        this.mSnapGravities.add(51);
        this.mSnapGravities.add(53);
        this.mSnapGravities.add(83);
        this.mSnapGravities.add(85);
    }

    public void dump(PrintWriter printWriter, String str) {
        String str2 = str + "  ";
        printWriter.println(str + PipSnapAlgorithm.class.getSimpleName());
        printWriter.println(str2 + "mSnapMode=" + this.mSnapMode);
        printWriter.println(str2 + "mOrientation=" + this.mOrientation);
        printWriter.println(str2 + "mMinimizedVisibleSize=" + this.mMinimizedVisibleSize);
        printWriter.println(str2 + "mIsMinimized=" + this.mIsMinimized);
    }
}
