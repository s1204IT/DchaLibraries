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

    public void insertColor(String accountName, String accountType, int displayColor, int colorKey) {
        this.mColorKeyMap.put(createKey(accountName, accountType, displayColor), Integer.valueOf(colorKey));
        String key = createKey(accountName, accountType);
        ArrayList<Integer> colorPalette = this.mColorPaletteMap.get(key);
        if (colorPalette == null) {
            colorPalette = new ArrayList<>();
        }
        colorPalette.add(Integer.valueOf(displayColor));
        this.mColorPaletteMap.put(key, colorPalette);
    }

    public int[] getColorArray(String accountName, String accountType) {
        ArrayList<Integer> colors = this.mColorPaletteMap.get(createKey(accountName, accountType));
        if (colors == null) {
            return null;
        }
        int[] ret = new int[colors.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = colors.get(i).intValue();
        }
        return ret;
    }

    public int getColorKey(String accountName, String accountType, int displayColor) {
        return this.mColorKeyMap.get(createKey(accountName, accountType, displayColor)).intValue();
    }

    public void sortPalettes(Comparator<Integer> comparator) {
        for (String key : this.mColorPaletteMap.keySet()) {
            ArrayList<Integer> palette = this.mColorPaletteMap.get(key);
            Integer[] sortedColors = new Integer[palette.size()];
            Arrays.sort(palette.toArray(sortedColors), comparator);
            palette.clear();
            for (Integer color : sortedColors) {
                palette.add(color);
            }
            this.mColorPaletteMap.put(key, palette);
        }
    }

    private String createKey(String accountName, String accountType) {
        return accountName + "::" + accountType;
    }

    private String createKey(String accountName, String accountType, int displayColor) {
        return createKey(accountName, accountType) + "::" + displayColor;
    }
}
