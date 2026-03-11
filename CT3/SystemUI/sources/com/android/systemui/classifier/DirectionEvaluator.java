package com.android.systemui.classifier;

public class DirectionEvaluator {
    public static float evaluate(float xDiff, float yDiff, int type) {
        boolean vertical = Math.abs(yDiff) >= Math.abs(xDiff);
        switch (type) {
            case 0:
            case 2:
                if (!vertical || yDiff <= 0.0d) {
                    return 5.5f;
                }
                return 0.0f;
            case 1:
                if (vertical) {
                    return 5.5f;
                }
                return 0.0f;
            case 3:
            default:
                return 0.0f;
            case 4:
                if (!vertical || yDiff >= 0.0d) {
                    return 5.5f;
                }
                return 0.0f;
            case 5:
                if (xDiff < 0.0d && yDiff > 0.0d) {
                    return 5.5f;
                }
                return 0.0f;
            case 6:
                if (xDiff > 0.0d && yDiff > 0.0d) {
                    return 5.5f;
                }
                return 0.0f;
        }
    }
}
