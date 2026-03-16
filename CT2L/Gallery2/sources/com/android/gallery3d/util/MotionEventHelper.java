package com.android.gallery3d.util;

import android.annotation.TargetApi;
import android.graphics.Matrix;
import android.util.FloatMath;
import android.view.MotionEvent;
import com.android.gallery3d.common.ApiHelper;

public final class MotionEventHelper {
    public static MotionEvent transformEvent(MotionEvent e, Matrix m) {
        return ApiHelper.HAS_MOTION_EVENT_TRANSFORM ? transformEventNew(e, m) : transformEventOld(e, m);
    }

    @TargetApi(11)
    private static MotionEvent transformEventNew(MotionEvent e, Matrix m) {
        MotionEvent newEvent = MotionEvent.obtain(e);
        newEvent.transform(m);
        return newEvent;
    }

    private static MotionEvent transformEventOld(MotionEvent e, Matrix m) {
        long downTime = e.getDownTime();
        long eventTime = e.getEventTime();
        int action = e.getAction();
        int pointerCount = e.getPointerCount();
        int[] pointerIds = getPointerIds(e);
        MotionEvent.PointerCoords[] pointerCoords = getPointerCoords(e);
        int metaState = e.getMetaState();
        float xPrecision = e.getXPrecision();
        float yPrecision = e.getYPrecision();
        int deviceId = e.getDeviceId();
        int edgeFlags = e.getEdgeFlags();
        int source = e.getSource();
        int flags = e.getFlags();
        float[] xy = new float[pointerCoords.length * 2];
        for (int i = 0; i < pointerCount; i++) {
            xy[i * 2] = pointerCoords[i].x;
            xy[(i * 2) + 1] = pointerCoords[i].y;
        }
        m.mapPoints(xy);
        for (int i2 = 0; i2 < pointerCount; i2++) {
            pointerCoords[i2].x = xy[i2 * 2];
            pointerCoords[i2].y = xy[(i2 * 2) + 1];
            pointerCoords[i2].orientation = transformAngle(m, pointerCoords[i2].orientation);
        }
        MotionEvent n = MotionEvent.obtain(downTime, eventTime, action, pointerCount, pointerIds, pointerCoords, metaState, xPrecision, yPrecision, deviceId, edgeFlags, source, flags);
        return n;
    }

    private static int[] getPointerIds(MotionEvent e) {
        int n = e.getPointerCount();
        int[] r = new int[n];
        for (int i = 0; i < n; i++) {
            r[i] = e.getPointerId(i);
        }
        return r;
    }

    private static MotionEvent.PointerCoords[] getPointerCoords(MotionEvent e) {
        int n = e.getPointerCount();
        MotionEvent.PointerCoords[] r = new MotionEvent.PointerCoords[n];
        for (int i = 0; i < n; i++) {
            r[i] = new MotionEvent.PointerCoords();
            e.getPointerCoords(i, r[i]);
        }
        return r;
    }

    private static float transformAngle(Matrix m, float angleRadians) {
        float[] v = {FloatMath.sin(angleRadians), -FloatMath.cos(angleRadians)};
        m.mapVectors(v);
        float result = (float) Math.atan2(v[0], -v[1]);
        if (result < -1.5707963267948966d) {
            return (float) (((double) result) + 3.141592653589793d);
        }
        if (result > 1.5707963267948966d) {
            return (float) (((double) result) - 3.141592653589793d);
        }
        return result;
    }
}
