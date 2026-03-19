package android.support.design.math;

public final class MathUtils {
    public static final float DEFAULT_EPSILON = 1.0E-4f;
    private static final float DEG_TO_RAD = 0.017453292f;
    private static final float RAD_TO_DEG = 57.295784f;

    private MathUtils() {
    }

    public static float abs(float v) {
        return v > 0.0f ? v : -v;
    }

    public static int constrain(int amount, int low, int high) {
        return amount < low ? low : amount > high ? high : amount;
    }

    public static long constrain(long amount, long low, long high) {
        return amount < low ? low : amount > high ? high : amount;
    }

    public static float constrain(float amount, float low, float high) {
        return amount < low ? low : amount > high ? high : amount;
    }

    public static int nearest(int amount, int low, int high) {
        return abs((float) (amount - low)) < abs((float) (amount - high)) ? low : high;
    }

    public static float nearest(float amount, float low, float high) {
        return abs(amount - low) < abs(amount - high) ? low : high;
    }

    public static float log(float a) {
        return (float) Math.log(a);
    }

    public static float exp(float a) {
        return (float) Math.exp(a);
    }

    public static float pow(float a, float b) {
        return (float) Math.pow(a, b);
    }

    public static float max(float a, float b) {
        return a > b ? a : b;
    }

    public static float max(int a, int b) {
        return a > b ? a : b;
    }

    public static float max(float a, float b, float c) {
        if (a > b) {
            if (a > c) {
                return a;
            }
        } else if (b > c) {
            return b;
        }
        return c;
    }

    public static float max(int a, int b, int c) {
        int i;
        if (a > b) {
            i = a > c ? a : c;
        } else if (b > c) {
            i = b;
        }
        return i;
    }

    public static float max(float a, float b, float c, float d) {
        return (a <= b || a <= c || a <= d) ? (b <= c || b <= d) ? c > d ? c : d : b : a;
    }

    public static float max(int a, int b, int c, int d) {
        return (a <= b || a <= c || a <= d) ? (b <= c || b <= d) ? c > d ? c : d : b : a;
    }

    public static float min(float a, float b) {
        return a < b ? a : b;
    }

    public static float min(int a, int b) {
        return a < b ? a : b;
    }

    public static float min(float a, float b, float c) {
        if (a < b) {
            if (a < c) {
                return a;
            }
        } else if (b < c) {
            return b;
        }
        return c;
    }

    public static float min(int a, int b, int c) {
        int i;
        if (a < b) {
            i = a < c ? a : c;
        } else if (b < c) {
            i = b;
        }
        return i;
    }

    public static float dist(float x1, float y1, float x2, float y2) {
        float x = x2 - x1;
        float y = y2 - y1;
        return (float) Math.hypot(x, y);
    }

    public static float dist(float x1, float y1, float z1, float x2, float y2, float z2) {
        float x = x2 - x1;
        float y = y2 - y1;
        float z = z2 - z1;
        return (float) Math.sqrt((x * x) + (y * y) + (z * z));
    }

    public static float mag(float a, float b) {
        return (float) Math.hypot(a, b);
    }

    public static float mag(float a, float b, float c) {
        return (float) Math.sqrt((a * a) + (b * b) + (c * c));
    }

    public static float sq(float v) {
        return v * v;
    }

    public static float sqrt(float value) {
        return (float) Math.sqrt(value);
    }

    public static float dot(float v1x, float v1y, float v2x, float v2y) {
        return (v1x * v2x) + (v1y * v2y);
    }

    public static float cross(float v1x, float v1y, float v2x, float v2y) {
        return (v1x * v2y) - (v1y * v2x);
    }

    public static float radians(float degrees) {
        return DEG_TO_RAD * degrees;
    }

    public static float degrees(float radians) {
        return RAD_TO_DEG * radians;
    }

    public static float acos(float value) {
        return (float) Math.acos(value);
    }

    public static float asin(float value) {
        return (float) Math.asin(value);
    }

    public static float atan(float value) {
        return (float) Math.atan(value);
    }

    public static float atan2(float a, float b) {
        return (float) Math.atan2(a, b);
    }

    public static float tan(float angle) {
        return (float) Math.tan(angle);
    }

    public static float lerp(float start, float stop, float amount) {
        return ((1.0f - amount) * start) + (amount * stop);
    }

    public static float norm(float start, float stop, float value) {
        return (value - start) / (stop - start);
    }

    public static float map(float minStart, float minStop, float maxStart, float maxStop, float value) {
        return ((maxStart - maxStop) * ((value - minStart) / (minStop - minStart))) + maxStart;
    }

    public static boolean leq(float a, float b, float epsilon) {
        return a <= b + epsilon;
    }

    public static boolean geq(float a, float b, float epsilon) {
        return a + epsilon >= b;
    }

    public static boolean eq(float a, float b, float epsilon) {
        return abs(a - b) <= epsilon;
    }

    public static boolean neq(float a, float b, float epsilon) {
        return abs(a - b) > epsilon;
    }

    public static float distanceToFurthestCorner(float pointX, float pointY, float rectLeft, float rectTop, float rectRight, float rectBottom) {
        return max(dist(pointX, pointY, rectLeft, rectTop), dist(pointX, pointY, rectRight, rectTop), dist(pointX, pointY, rectRight, rectBottom), dist(pointX, pointY, rectLeft, rectBottom));
    }
}
