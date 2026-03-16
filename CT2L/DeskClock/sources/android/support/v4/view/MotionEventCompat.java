package android.support.v4.view;

import android.os.Build;
import android.view.MotionEvent;

public class MotionEventCompat {
    static final MotionEventVersionImpl IMPL;

    interface MotionEventVersionImpl {
        int findPointerIndex(MotionEvent motionEvent, int i);

        int getPointerCount(MotionEvent motionEvent);

        int getPointerId(MotionEvent motionEvent, int i);

        float getX(MotionEvent motionEvent, int i);

        float getY(MotionEvent motionEvent, int i);
    }

    static class BaseMotionEventVersionImpl implements MotionEventVersionImpl {
        BaseMotionEventVersionImpl() {
        }

        @Override
        public int findPointerIndex(MotionEvent event, int pointerId) {
            return pointerId == 0 ? 0 : -1;
        }

        @Override
        public int getPointerId(MotionEvent event, int pointerIndex) {
            if (pointerIndex == 0) {
                return 0;
            }
            throw new IndexOutOfBoundsException("Pre-Eclair does not support multiple pointers");
        }

        @Override
        public float getX(MotionEvent event, int pointerIndex) {
            if (pointerIndex == 0) {
                return event.getX();
            }
            throw new IndexOutOfBoundsException("Pre-Eclair does not support multiple pointers");
        }

        @Override
        public float getY(MotionEvent event, int pointerIndex) {
            if (pointerIndex == 0) {
                return event.getY();
            }
            throw new IndexOutOfBoundsException("Pre-Eclair does not support multiple pointers");
        }

        @Override
        public int getPointerCount(MotionEvent event) {
            return 1;
        }
    }

    static class EclairMotionEventVersionImpl implements MotionEventVersionImpl {
        EclairMotionEventVersionImpl() {
        }

        @Override
        public int findPointerIndex(MotionEvent event, int pointerId) {
            return MotionEventCompatEclair.findPointerIndex(event, pointerId);
        }

        @Override
        public int getPointerId(MotionEvent event, int pointerIndex) {
            return MotionEventCompatEclair.getPointerId(event, pointerIndex);
        }

        @Override
        public float getX(MotionEvent event, int pointerIndex) {
            return MotionEventCompatEclair.getX(event, pointerIndex);
        }

        @Override
        public float getY(MotionEvent event, int pointerIndex) {
            return MotionEventCompatEclair.getY(event, pointerIndex);
        }

        @Override
        public int getPointerCount(MotionEvent event) {
            return MotionEventCompatEclair.getPointerCount(event);
        }
    }

    static {
        if (Build.VERSION.SDK_INT >= 5) {
            IMPL = new EclairMotionEventVersionImpl();
        } else {
            IMPL = new BaseMotionEventVersionImpl();
        }
    }

    public static int getActionMasked(MotionEvent event) {
        return event.getAction() & 255;
    }

    public static int getActionIndex(MotionEvent event) {
        return (event.getAction() & 65280) >> 8;
    }

    public static int findPointerIndex(MotionEvent event, int pointerId) {
        return IMPL.findPointerIndex(event, pointerId);
    }

    public static int getPointerId(MotionEvent event, int pointerIndex) {
        return IMPL.getPointerId(event, pointerIndex);
    }

    public static float getX(MotionEvent event, int pointerIndex) {
        return IMPL.getX(event, pointerIndex);
    }

    public static float getY(MotionEvent event, int pointerIndex) {
        return IMPL.getY(event, pointerIndex);
    }

    public static int getPointerCount(MotionEvent event) {
        return IMPL.getPointerCount(event);
    }
}
