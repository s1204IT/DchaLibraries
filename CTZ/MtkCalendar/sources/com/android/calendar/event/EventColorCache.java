package com.android.calendar.event;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class EventColorCache implements Serializable {
    private static final long serialVersionUID = 2;
    private Map<String, ArrayList<Integer>> mColorPaletteMap = new HashMap();
    private Map<String, Integer> mColorKeyMap = new HashMap();

    public void insertColor(String str, String str2, int i, int i2) {
        this.mColorKeyMap.put(createKey(str, str2, i), Integer.valueOf(i2));
        String strCreateKey = createKey(str, str2);
        ArrayList<Integer> arrayList = this.mColorPaletteMap.get(strCreateKey);
        if (arrayList == null) {
            arrayList = new ArrayList<>();
        }
        arrayList.add(Integer.valueOf(i));
        this.mColorPaletteMap.put(strCreateKey, arrayList);
    }

    public int[] getColorArray(String str, String str2) {
        ArrayList<Integer> arrayList = this.mColorPaletteMap.get(createKey(str, str2));
        if (arrayList == null) {
            return null;
        }
        int[] iArr = new int[arrayList.size()];
        for (int i = 0; i < iArr.length; i++) {
            iArr[i] = arrayList.get(i).intValue();
        }
        return iArr;
    }

    public int getColorKey(String str, String str2, int i) {
        return this.mColorKeyMap.get(createKey(str, str2, i)).intValue();
    }

    public void sortPalettes(Comparator<Integer> comparator) {
        for (String str : this.mColorPaletteMap.keySet()) {
            ArrayList<Integer> arrayList = this.mColorPaletteMap.get(str);
            Integer[] numArr = new Integer[arrayList.size()];
            Arrays.sort((Integer[]) arrayList.toArray(numArr), comparator);
            arrayList.clear();
            for (Integer num : numArr) {
                arrayList.add(num);
            }
            this.mColorPaletteMap.put(str, arrayList);
        }
    }

    private String createKey(String str, String str2) {
        return str + "::" + str2;
    }

    private String createKey(String str, String str2, int i) {
        return createKey(str, str2) + "::" + i;
    }
}
