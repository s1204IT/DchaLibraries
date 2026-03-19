package com.android.printspooler.util;

import android.print.PageRange;
import android.util.Pair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public final class PageRangeUtils {
    private static final PageRange[] ALL_PAGES_RANGE = {PageRange.ALL_PAGES};
    private static final Comparator<PageRange> sComparator = new Comparator<PageRange>() {
        @Override
        public int compare(PageRange pageRange, PageRange pageRange2) {
            return pageRange.getStart() - pageRange2.getStart();
        }
    };

    public static boolean contains(PageRange[] pageRangeArr, int i) {
        for (PageRange pageRange : pageRangeArr) {
            if (pageRange.contains(i)) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(PageRange[] pageRangeArr, PageRange[] pageRangeArr2, int i) {
        if (pageRangeArr == null || pageRangeArr2 == null) {
            return false;
        }
        if (Arrays.equals(pageRangeArr, ALL_PAGES_RANGE)) {
            return true;
        }
        if (Arrays.equals(pageRangeArr2, ALL_PAGES_RANGE)) {
            pageRangeArr2[0] = new PageRange(0, i - 1);
        }
        PageRange[] pageRangeArrNormalize = normalize(pageRangeArr);
        PageRange[] pageRangeArrNormalize2 = normalize(pageRangeArr2);
        int length = pageRangeArrNormalize2.length;
        int i2 = 0;
        for (PageRange pageRange : pageRangeArrNormalize) {
            while (i2 < length) {
                PageRange pageRange2 = pageRangeArrNormalize2[i2];
                if (pageRange2.getStart() > pageRange.getEnd()) {
                    break;
                }
                if (pageRange2.getStart() < pageRange.getStart() || pageRange2.getEnd() > pageRange.getEnd()) {
                    return false;
                }
                i2++;
            }
        }
        return i2 >= length;
    }

    public static PageRange[] normalize(PageRange[] pageRangeArr) {
        if (pageRangeArr == null) {
            return null;
        }
        int length = pageRangeArr.length;
        if (length <= 1) {
            return pageRangeArr;
        }
        Arrays.sort(pageRangeArr, sComparator);
        int i = 1;
        int i2 = 0;
        while (i2 < length - 1) {
            PageRange pageRange = pageRangeArr[i2];
            int i3 = i2 + 1;
            PageRange pageRange2 = pageRangeArr[i3];
            if (pageRange.getEnd() + 1 >= pageRange2.getStart()) {
                pageRangeArr[i2] = null;
                pageRangeArr[i3] = new PageRange(pageRange.getStart(), Math.max(pageRange.getEnd(), pageRange2.getEnd()));
            } else {
                i++;
            }
            i2 = i3;
        }
        if (i == length) {
            return pageRangeArr;
        }
        PageRange[] pageRangeArr2 = new PageRange[i];
        int i4 = 0;
        for (PageRange pageRange3 : pageRangeArr) {
            if (pageRange3 != null) {
                pageRangeArr2[i4] = pageRange3;
                i4++;
            }
        }
        return pageRangeArr2;
    }

    private static int readWhiteSpace(CharSequence charSequence, int i) {
        while (i < charSequence.length() && charSequence.charAt(i) == ' ') {
            i++;
        }
        return i;
    }

    private static Pair<Integer, Integer> readNumber(CharSequence charSequence, int i) {
        Integer numValueOf = 0;
        while (i < charSequence.length() && charSequence.charAt(i) >= '0' && charSequence.charAt(i) <= '9' && (numValueOf.intValue() != 0 || charSequence.charAt(i) != '0')) {
            numValueOf = Integer.valueOf((numValueOf.intValue() * 10) + (charSequence.charAt(i) - '0'));
            if (numValueOf.intValue() < 0) {
                break;
            }
            i++;
        }
        if (numValueOf.intValue() == 0) {
            return new Pair<>(Integer.valueOf(i), null);
        }
        return new Pair<>(Integer.valueOf(i), numValueOf);
    }

    private static Pair<Integer, Character> readChar(CharSequence charSequence, int i, char c) {
        if (i < charSequence.length() && charSequence.charAt(i) == c) {
            return new Pair<>(Integer.valueOf(i + 1), Character.valueOf(c));
        }
        return new Pair<>(Integer.valueOf(i), null);
    }

    private static Pair<Integer, PageRange> readRange(CharSequence charSequence, int i, int i2) {
        Character ch;
        if (i == 0) {
            ch = ',';
        } else {
            Pair<Integer, Character> pair = readChar(charSequence, i, ',');
            int iIntValue = ((Integer) pair.first).intValue();
            ch = (Character) pair.second;
            i = iIntValue;
        }
        Pair<Integer, Integer> number = readNumber(charSequence, readWhiteSpace(charSequence, i));
        int iIntValue2 = ((Integer) number.first).intValue();
        Integer num = (Integer) number.second;
        Pair<Integer, Character> pair2 = readChar(charSequence, readWhiteSpace(charSequence, iIntValue2), '-');
        int iIntValue3 = ((Integer) pair2.first).intValue();
        Character ch2 = (Character) pair2.second;
        Pair<Integer, Integer> number2 = readNumber(charSequence, readWhiteSpace(charSequence, iIntValue3));
        int iIntValue4 = ((Integer) number2.first).intValue();
        Integer numValueOf = (Integer) number2.second;
        int whiteSpace = readWhiteSpace(charSequence, iIntValue4);
        if (ch != null && ((ch2 != null && (num != null || numValueOf != null)) || (ch2 == null && num != null && numValueOf == null))) {
            if (num == null) {
                num = 1;
            }
            if (numValueOf == null) {
                if (ch2 != null) {
                    numValueOf = Integer.valueOf(i2);
                } else {
                    numValueOf = num;
                }
            }
            if (num.intValue() <= numValueOf.intValue() && num.intValue() >= 1 && numValueOf.intValue() <= i2) {
                return new Pair<>(Integer.valueOf(whiteSpace), new PageRange(num.intValue() - 1, numValueOf.intValue() - 1));
            }
        }
        return new Pair<>(Integer.valueOf(whiteSpace), null);
    }

    public static PageRange[] parsePageRanges(CharSequence charSequence, int i) {
        ArrayList arrayList = new ArrayList();
        int iIntValue = 0;
        while (true) {
            if (iIntValue >= charSequence.length()) {
                break;
            }
            Pair<Integer, PageRange> range = readRange(charSequence, iIntValue, i);
            if (range.second == null) {
                arrayList.clear();
                break;
            }
            arrayList.add((PageRange) range.second);
            iIntValue = ((Integer) range.first).intValue();
        }
        return normalize((PageRange[]) arrayList.toArray(new PageRange[arrayList.size()]));
    }

    public static void offset(PageRange[] pageRangeArr, int i) {
        if (i == 0) {
            return;
        }
        int length = pageRangeArr.length;
        for (int i2 = 0; i2 < length; i2++) {
            pageRangeArr[i2] = new PageRange(pageRangeArr[i2].getStart() + i, pageRangeArr[i2].getEnd() + i);
        }
    }

    public static int getNormalizedPageCount(PageRange[] pageRangeArr, int i) {
        if (pageRangeArr == null) {
            return 0;
        }
        int size = 0;
        for (PageRange pageRange : pageRangeArr) {
            if (PageRange.ALL_PAGES.equals(pageRange)) {
                return i;
            }
            size += pageRange.getSize();
        }
        return size;
    }

    public static PageRange asAbsoluteRange(PageRange pageRange, int i) {
        if (PageRange.ALL_PAGES.equals(pageRange)) {
            return new PageRange(0, i - 1);
        }
        return pageRange;
    }

    public static boolean isAllPages(PageRange[] pageRangeArr) {
        for (PageRange pageRange : pageRangeArr) {
            if (isAllPages(pageRange)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAllPages(PageRange pageRange) {
        return PageRange.ALL_PAGES.equals(pageRange);
    }

    public static boolean isAllPages(PageRange[] pageRangeArr, int i) {
        for (PageRange pageRange : pageRangeArr) {
            if (isAllPages(pageRange, i)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAllPages(PageRange pageRange, int i) {
        return pageRange.getStart() == 0 && pageRange.getEnd() == i - 1;
    }

    public static PageRange[] computeWhichPagesInFileToPrint(PageRange[] pageRangeArr, PageRange[] pageRangeArr2, int i) {
        if (Arrays.equals(pageRangeArr, ALL_PAGES_RANGE) && i == -1) {
            return ALL_PAGES_RANGE;
        }
        if (Arrays.equals(pageRangeArr2, pageRangeArr)) {
            return ALL_PAGES_RANGE;
        }
        if (Arrays.equals(pageRangeArr2, ALL_PAGES_RANGE)) {
            return pageRangeArr;
        }
        if (contains(pageRangeArr2, pageRangeArr, i)) {
            offset((PageRange[]) pageRangeArr.clone(), -pageRangeArr2[0].getStart());
            return pageRangeArr;
        }
        if (Arrays.equals(pageRangeArr, ALL_PAGES_RANGE) && isAllPages(pageRangeArr2, i)) {
            return ALL_PAGES_RANGE;
        }
        return null;
    }
}
