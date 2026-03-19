package com.android.server.accessibility;

import android.util.MathUtils;
import android.view.MotionEvent;

final class GestureUtils {
    private GestureUtils() {
    }

    public static boolean isMultiTap(MotionEvent motionEvent, MotionEvent motionEvent2, int i, int i2) {
        if (motionEvent == null || motionEvent2 == null) {
            return false;
        }
        return eventsWithinTimeAndDistanceSlop(motionEvent, motionEvent2, i, i2);
    }

    private static boolean eventsWithinTimeAndDistanceSlop(MotionEvent motionEvent, MotionEvent motionEvent2, int i, int i2) {
        return !isTimedOut(motionEvent, motionEvent2, i) && distance(motionEvent, motionEvent2) < ((double) i2);
    }

    public static double distance(MotionEvent motionEvent, MotionEvent motionEvent2) {
        return MathUtils.dist(motionEvent.getX(), motionEvent.getY(), motionEvent2.getX(), motionEvent2.getY());
    }

    public static boolean isTimedOut(MotionEvent motionEvent, MotionEvent motionEvent2, int i) {
        return motionEvent2.getEventTime() - motionEvent.getEventTime() >= ((long) i);
    }

    public static boolean isDraggingGesture(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9) {
        float f10 = f5 - f;
        float f11 = f6 - f2;
        if (f10 == 0.0f && f11 == 0.0f) {
            return true;
        }
        float fHypot = (float) Math.hypot(f10, f11);
        if (fHypot > 0.0f) {
            f10 /= fHypot;
        }
        if (fHypot > 0.0f) {
            f11 /= fHypot;
        }
        float f12 = f7 - f3;
        float f13 = f8 - f4;
        if (f12 == 0.0f && f13 == 0.0f) {
            return true;
        }
        float fHypot2 = (float) Math.hypot(f12, f13);
        if (fHypot2 > 0.0f) {
            f12 /= fHypot2;
        }
        if (fHypot2 > 0.0f) {
            f13 /= fHypot2;
        }
        return (f10 * f12) + (f11 * f13) >= f9;
    }
}
