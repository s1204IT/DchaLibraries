package android.ext.services.autofill;

import android.util.Log;
import android.view.autofill.AutofillValue;
import com.android.internal.annotations.VisibleForTesting;
import java.lang.reflect.Array;
import java.util.List;

final class EditDistanceScorer {
    @VisibleForTesting
    static float getScore(AutofillValue autofillValue, String str) {
        if (autofillValue == null || !autofillValue.isText() || str == null) {
            return 0.0f;
        }
        String string = autofillValue.getTextValue().toString();
        int length = string.length();
        int length2 = str.length();
        if (length2 == 0) {
            return length == 0 ? 1.0f : 0.0f;
        }
        int iEditDistance = editDistance(string.toLowerCase(), str.toLowerCase());
        float fMax = Math.max(length, length2);
        return (fMax - iEditDistance) / fMax;
    }

    public static int editDistance(String str, String str2) {
        return editDistance(str, str2, Integer.MAX_VALUE);
    }

    private static int editDistance(String str, String str2, int i) {
        if (str.equals(str2)) {
            return 0;
        }
        if (Math.abs(str.length() - str2.length()) > i) {
            return Integer.MAX_VALUE;
        }
        int length = str.length();
        int length2 = str2.length();
        int[][] iArr = (int[][]) Array.newInstance((Class<?>) int.class, length + 1, length2 + 1);
        for (int i2 = 0; i2 <= length; i2++) {
            iArr[i2][0] = i2;
        }
        for (int i3 = 0; i3 <= length2; i3++) {
            iArr[0][i3] = i3;
        }
        for (int i4 = 1; i4 <= length2; i4++) {
            for (int i5 = 1; i5 <= length; i5++) {
                int i6 = i5 - 1;
                int i7 = i4 - 1;
                if (str.charAt(i6) == str2.charAt(i7)) {
                    iArr[i5][i4] = iArr[i6][i7];
                } else {
                    iArr[i5][i4] = Math.min(iArr[i6][i4] + 1, Math.min(iArr[i5][i7] + 1, iArr[i6][i7] + 1));
                }
            }
        }
        return iArr[length][length2];
    }

    static float[][] getScores(List<AutofillValue> list, List<String> list2) {
        int size = list.size();
        int size2 = list2.size();
        Log.d("EditDistanceScorer", "getScores() will return a " + size + "x" + size2 + " matrix for EDIT_DISTANCE");
        float[][] fArr = (float[][]) Array.newInstance((Class<?>) float.class, size, size2);
        for (int i = 0; i < size; i++) {
            for (int i2 = 0; i2 < size2; i2++) {
                fArr[i][i2] = getScore(list.get(i), list2.get(i2));
            }
        }
        return fArr;
    }
}
