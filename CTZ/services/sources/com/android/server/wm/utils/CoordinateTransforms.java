package com.android.server.wm.utils;

import android.graphics.Matrix;

public class CoordinateTransforms {
    private CoordinateTransforms() {
    }

    public static void transformPhysicalToLogicalCoordinates(int i, int i2, int i3, Matrix matrix) {
        switch (i) {
            case 0:
                matrix.reset();
                return;
            case 1:
                matrix.setRotate(270.0f);
                matrix.postTranslate(0.0f, i2);
                return;
            case 2:
                matrix.setRotate(180.0f);
                matrix.postTranslate(i2, i3);
                return;
            case 3:
                matrix.setRotate(90.0f);
                matrix.postTranslate(i3, 0.0f);
                return;
            default:
                throw new IllegalArgumentException("Unknown rotation: " + i);
        }
    }
}
