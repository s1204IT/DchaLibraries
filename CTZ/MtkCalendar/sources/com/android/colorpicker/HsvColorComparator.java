package com.android.colorpicker;

import android.graphics.Color;
import java.util.Comparator;

public class HsvColorComparator implements Comparator<Integer> {
    @Override
    public int compare(Integer num, Integer num2) {
        float[] fArr = new float[3];
        Color.colorToHSV(num.intValue(), fArr);
        float f = fArr[0];
        float f2 = fArr[1];
        float f3 = fArr[2];
        float[] fArr2 = new float[3];
        Color.colorToHSV(num2.intValue(), fArr2);
        float f4 = fArr2[0];
        float f5 = fArr2[1];
        float f6 = fArr2[2];
        if (f < f4) {
            return 1;
        }
        if (f > f4) {
            return -1;
        }
        if (f2 < f5) {
            return 1;
        }
        if (f2 > f5) {
            return -1;
        }
        if (f3 < f6) {
            return 1;
        }
        return f3 > f6 ? -1 : 0;
    }
}
