package com.android.gallery3d.util;

import android.annotation.TargetApi;
import android.graphics.Matrix;
import android.view.MotionEvent;
import com.android.gallery3d.common.ApiHelper;

public final class MotionEventHelper {
    public static MotionEvent transformEvent(MotionEvent motionEvent, Matrix matrix) {
        if (ApiHelper.HAS_MOTION_EVENT_TRANSFORM) {
            return transformEventNew(motionEvent, matrix);
        }
        return transformEventOld(motionEvent, matrix);
    }

    @TargetApi(11)
    private static MotionEvent transformEventNew(MotionEvent motionEvent, Matrix matrix) {
        MotionEvent motionEventObtain = MotionEvent.obtain(motionEvent);
        motionEventObtain.transform(matrix);
        return motionEventObtain;
    }

    private static MotionEvent transformEventOld(MotionEvent motionEvent, Matrix matrix) {
        long downTime = motionEvent.getDownTime();
        long eventTime = motionEvent.getEventTime();
        int action = motionEvent.getAction();
        int pointerCount = motionEvent.getPointerCount();
        int[] pointerIds = getPointerIds(motionEvent);
        MotionEvent.PointerCoords[] pointerCoords = getPointerCoords(motionEvent);
        int metaState = motionEvent.getMetaState();
        float xPrecision = motionEvent.getXPrecision();
        float yPrecision = motionEvent.getYPrecision();
        int deviceId = motionEvent.getDeviceId();
        int edgeFlags = motionEvent.getEdgeFlags();
        int source = motionEvent.getSource();
        int flags = motionEvent.getFlags();
        float[] fArr = new float[pointerCoords.length * 2];
        int i = 0;
        while (i < pointerCount) {
            int i2 = 2 * i;
            fArr[i2] = pointerCoords[i].x;
            fArr[i2 + 1] = pointerCoords[i].y;
            i++;
            edgeFlags = edgeFlags;
        }
        int i3 = edgeFlags;
        matrix.mapPoints(fArr);
        int i4 = 0;
        while (i4 < pointerCount) {
            int i5 = 2 * i4;
            pointerCoords[i4].x = fArr[i5];
            pointerCoords[i4].y = fArr[i5 + 1];
            pointerCoords[i4].orientation = transformAngle(matrix, pointerCoords[i4].orientation);
            i4++;
            deviceId = deviceId;
        }
        return MotionEvent.obtain(downTime, eventTime, action, pointerCount, pointerIds, pointerCoords, metaState, xPrecision, yPrecision, deviceId, i3, source, flags);
    }

    private static int[] getPointerIds(MotionEvent motionEvent) {
        int pointerCount = motionEvent.getPointerCount();
        int[] iArr = new int[pointerCount];
        for (int i = 0; i < pointerCount; i++) {
            iArr[i] = motionEvent.getPointerId(i);
        }
        return iArr;
    }

    private static MotionEvent.PointerCoords[] getPointerCoords(MotionEvent motionEvent) {
        int pointerCount = motionEvent.getPointerCount();
        MotionEvent.PointerCoords[] pointerCoordsArr = new MotionEvent.PointerCoords[pointerCount];
        for (int i = 0; i < pointerCount; i++) {
            pointerCoordsArr[i] = new MotionEvent.PointerCoords();
            motionEvent.getPointerCoords(i, pointerCoordsArr[i]);
        }
        return pointerCoordsArr;
    }

    private static float transformAngle(Matrix matrix, float f) {
        double d = f;
        matrix.mapVectors(new float[]{(float) Math.sin(d), (float) (-Math.cos(d))});
        float fAtan2 = (float) Math.atan2(r0[0], -r0[1]);
        double d2 = fAtan2;
        if (d2 < -1.5707963267948966d) {
            return (float) (d2 + 3.141592653589793d);
        }
        if (d2 > 1.5707963267948966d) {
            return (float) (d2 - 3.141592653589793d);
        }
        return fAtan2;
    }
}
