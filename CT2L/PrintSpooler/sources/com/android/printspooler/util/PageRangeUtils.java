package com.android.printspooler.util;

import android.print.PageRange;
import java.util.Arrays;
import java.util.Comparator;

public final class PageRangeUtils {
    private static final PageRange[] ALL_PAGES_RANGE = {PageRange.ALL_PAGES};
    private static final Comparator<PageRange> sComparator = new Comparator<PageRange>() {
        @Override
        public int compare(PageRange lhs, PageRange rhs) {
            return lhs.getStart() - rhs.getStart();
        }
    };

    public static boolean contains(PageRange[] pageRanges, int pageIndex) {
        for (PageRange pageRange : pageRanges) {
            if (pageRange.contains(pageIndex)) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(PageRange[] ourRanges, PageRange[] otherRanges, int pageCount) {
        if (ourRanges == null || otherRanges == null) {
            return false;
        }
        if (Arrays.equals(ourRanges, ALL_PAGES_RANGE)) {
            return true;
        }
        if (Arrays.equals(otherRanges, ALL_PAGES_RANGE)) {
            otherRanges[0] = new PageRange(0, pageCount - 1);
        }
        PageRange[] ourRanges2 = normalize(ourRanges);
        PageRange[] otherRanges2 = normalize(otherRanges);
        int otherRangeIdx = 0;
        int otherRangeCount = otherRanges2.length;
        for (PageRange ourRange : ourRanges2) {
            while (otherRangeIdx < otherRangeCount) {
                PageRange otherRange = otherRanges2[otherRangeIdx];
                if (otherRange.getStart() > ourRange.getEnd()) {
                    break;
                }
                if (otherRange.getStart() < ourRange.getStart() || otherRange.getEnd() > ourRange.getEnd()) {
                    return false;
                }
                otherRangeIdx++;
            }
        }
        return otherRangeIdx >= otherRangeCount;
    }

    public static PageRange[] normalize(PageRange[] pageRanges) {
        if (pageRanges == null) {
            return null;
        }
        int oldRangeCount = pageRanges.length;
        if (oldRangeCount > 1) {
            Arrays.sort(pageRanges, sComparator);
            int newRangeCount = 1;
            for (int i = 0; i < oldRangeCount - 1; i++) {
                PageRange currentRange = pageRanges[i];
                PageRange nextRange = pageRanges[i + 1];
                if (currentRange.getEnd() + 1 >= nextRange.getStart()) {
                    pageRanges[i] = null;
                    pageRanges[i + 1] = new PageRange(currentRange.getStart(), Math.max(currentRange.getEnd(), nextRange.getEnd()));
                } else {
                    newRangeCount++;
                }
            }
            if (newRangeCount != oldRangeCount) {
                int normalRangeIndex = 0;
                PageRange[] normalRanges = new PageRange[newRangeCount];
                for (PageRange normalRange : pageRanges) {
                    if (normalRange != null) {
                        normalRanges[normalRangeIndex] = normalRange;
                        normalRangeIndex++;
                    }
                }
                return normalRanges;
            }
            return pageRanges;
        }
        return pageRanges;
    }

    public static void offset(PageRange[] pageRanges, int offset) {
        if (offset != 0) {
            int pageRangeCount = pageRanges.length;
            for (int i = 0; i < pageRangeCount; i++) {
                int start = pageRanges[i].getStart() + offset;
                int end = pageRanges[i].getEnd() + offset;
                pageRanges[i] = new PageRange(start, end);
            }
        }
    }

    public static int getNormalizedPageCount(PageRange[] pageRanges, int layoutPageCount) {
        int pageCount = 0;
        if (pageRanges != null) {
            for (PageRange pageRange : pageRanges) {
                if (!PageRange.ALL_PAGES.equals(pageRange)) {
                    pageCount += pageRange.getSize();
                } else {
                    return layoutPageCount;
                }
            }
        }
        int layoutPageCount2 = pageCount;
        return layoutPageCount2;
    }

    public static PageRange asAbsoluteRange(PageRange pageRange, int pageCount) {
        if (PageRange.ALL_PAGES.equals(pageRange)) {
            return new PageRange(0, pageCount - 1);
        }
        return pageRange;
    }

    public static boolean isAllPages(PageRange[] pageRanges) {
        for (PageRange pageRange : pageRanges) {
            if (isAllPages(pageRange)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAllPages(PageRange pageRange) {
        return PageRange.ALL_PAGES.equals(pageRange);
    }

    public static boolean isAllPages(PageRange[] pageRanges, int pageCount) {
        for (PageRange pageRange : pageRanges) {
            if (isAllPages(pageRange, pageCount)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAllPages(PageRange pageRanges, int pageCount) {
        return pageRanges.getStart() == 0 && pageRanges.getEnd() == pageCount + (-1);
    }

    public static PageRange[] computePrintedPages(PageRange[] requestedPages, PageRange[] writtenPages, int pageCount) {
        if (Arrays.equals(requestedPages, ALL_PAGES_RANGE) && pageCount == -1) {
            return ALL_PAGES_RANGE;
        }
        if (Arrays.equals(writtenPages, requestedPages)) {
            return ALL_PAGES_RANGE;
        }
        if (!Arrays.equals(writtenPages, ALL_PAGES_RANGE)) {
            if (contains(writtenPages, requestedPages, pageCount)) {
                int offset = -writtenPages[0].getStart();
                offset(requestedPages, offset);
                return requestedPages;
            }
            if (Arrays.equals(requestedPages, ALL_PAGES_RANGE) && isAllPages(writtenPages, pageCount)) {
                return ALL_PAGES_RANGE;
            }
            return null;
        }
        return requestedPages;
    }
}
