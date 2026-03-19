package android.graphics;

import android.util.Log;

public class Atlas {

    private static final int[] f9androidgraphicsAtlas$TypeSwitchesValues = null;
    public static final int FLAG_ADD_PADDING = 2;
    public static final int FLAG_DEFAULTS = 2;
    private final Policy mPolicy;

    public static class Entry {
        public int x;
        public int y;
    }

    private static int[] m593getandroidgraphicsAtlas$TypeSwitchesValues() {
        if (f9androidgraphicsAtlas$TypeSwitchesValues != null) {
            return f9androidgraphicsAtlas$TypeSwitchesValues;
        }
        int[] iArr = new int[Type.valuesCustom().length];
        try {
            iArr[Type.SliceLongAxis.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[Type.SliceMaxArea.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[Type.SliceMinArea.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[Type.SliceShortAxis.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        f9androidgraphicsAtlas$TypeSwitchesValues = iArr;
        return iArr;
    }

    public enum Type {
        SliceMinArea,
        SliceMaxArea,
        SliceShortAxis,
        SliceLongAxis;

        public static Type[] valuesCustom() {
            return values();
        }
    }

    public Atlas(Type type, int width, int height) {
        this(type, width, height, 2);
    }

    public Atlas(Type type, int width, int height, int flags) {
        this.mPolicy = findPolicy(type, width, height, flags);
    }

    public Entry pack(int width, int height) {
        return pack(width, height, null);
    }

    public Entry pack(int width, int height, Entry entry) {
        if (entry == null) {
            entry = new Entry();
        }
        if (this.mPolicy == null) {
            return null;
        }
        return this.mPolicy.pack(width, height, entry);
    }

    private static Policy findPolicy(Type type, int width, int height, int flags) {
        try {
        } catch (Exception e) {
            Log.e("Atlas", "Exception when find policy", e);
        }
        switch (m593getandroidgraphicsAtlas$TypeSwitchesValues()[type.ordinal()]) {
            case 1:
                SlicePolicy.SplitDecision decision = new SlicePolicy.LongerFreeAxisSplitDecision(null);
                return new SlicePolicy(width, height, flags, decision);
            case 2:
                SlicePolicy.SplitDecision decision2 = new SlicePolicy.MaxAreaSplitDecision(null);
                return new SlicePolicy(width, height, flags, decision2);
            case 3:
                SlicePolicy.SplitDecision decision3 = new SlicePolicy.MinAreaSplitDecision(null);
                return new SlicePolicy(width, height, flags, decision3);
            case 4:
                SlicePolicy.SplitDecision decision4 = new SlicePolicy.ShorterFreeAxisSplitDecision(null);
                return new SlicePolicy(width, height, flags, decision4);
            default:
                Log.w("Atlas", "Incorrect type " + type + " in find policy on " + Thread.currentThread().getName());
                return null;
        }
    }

    private static abstract class Policy {
        Policy(Policy policy) {
            this();
        }

        abstract Entry pack(int i, int i2, Entry entry);

        private Policy() {
        }
    }

    private static class SlicePolicy extends Policy {
        private final int mPadding;
        private final Cell mRoot;
        private final SplitDecision mSplitDecision;

        private interface SplitDecision {
            boolean splitHorizontal(int i, int i2, int i3, int i4);
        }

        private static class Cell {
            int height;
            Cell next;
            int width;
            int x;
            int y;

            Cell(Cell cell) {
                this();
            }

            private Cell() {
            }

            public String toString() {
                return String.format("cell[x=%d y=%d width=%d height=%d", Integer.valueOf(this.x), Integer.valueOf(this.y), Integer.valueOf(this.width), Integer.valueOf(this.height));
            }
        }

        SlicePolicy(int i, int i2, int i3, SplitDecision splitDecision) {
            super(null);
            Object[] objArr = 0;
            this.mRoot = new Cell(0 == true ? 1 : 0);
            this.mPadding = (i3 & 2) != 0 ? 1 : 0;
            Cell cell = new Cell(objArr == true ? 1 : 0);
            int i4 = this.mPadding;
            cell.y = i4;
            cell.x = i4;
            cell.width = i - (this.mPadding * 2);
            cell.height = i2 - (this.mPadding * 2);
            this.mRoot.next = cell;
            this.mSplitDecision = splitDecision;
        }

        @Override
        Entry pack(int width, int height, Entry entry) {
            Cell prev = this.mRoot;
            for (Cell cell = this.mRoot.next; cell != null; cell = cell.next) {
                if (insert(cell, prev, width, height, entry)) {
                    return entry;
                }
                prev = cell;
            }
            return null;
        }

        private static class MinAreaSplitDecision implements SplitDecision {
            MinAreaSplitDecision(MinAreaSplitDecision minAreaSplitDecision) {
                this();
            }

            private MinAreaSplitDecision() {
            }

            @Override
            public boolean splitHorizontal(int freeWidth, int freeHeight, int rectWidth, int rectHeight) {
                return rectWidth * freeHeight > freeWidth * rectHeight;
            }
        }

        private static class MaxAreaSplitDecision implements SplitDecision {
            MaxAreaSplitDecision(MaxAreaSplitDecision maxAreaSplitDecision) {
                this();
            }

            private MaxAreaSplitDecision() {
            }

            @Override
            public boolean splitHorizontal(int freeWidth, int freeHeight, int rectWidth, int rectHeight) {
                return rectWidth * freeHeight <= freeWidth * rectHeight;
            }
        }

        private static class ShorterFreeAxisSplitDecision implements SplitDecision {
            ShorterFreeAxisSplitDecision(ShorterFreeAxisSplitDecision shorterFreeAxisSplitDecision) {
                this();
            }

            private ShorterFreeAxisSplitDecision() {
            }

            @Override
            public boolean splitHorizontal(int freeWidth, int freeHeight, int rectWidth, int rectHeight) {
                return freeWidth <= freeHeight;
            }
        }

        private static class LongerFreeAxisSplitDecision implements SplitDecision {
            LongerFreeAxisSplitDecision(LongerFreeAxisSplitDecision longerFreeAxisSplitDecision) {
                this();
            }

            private LongerFreeAxisSplitDecision() {
            }

            @Override
            public boolean splitHorizontal(int freeWidth, int freeHeight, int rectWidth, int rectHeight) {
                return freeWidth > freeHeight;
            }
        }

        private boolean insert(Cell cell, Cell prev, int width, int height, Entry entry) {
            Cell cell2 = null;
            if (cell.width < width || cell.height < height) {
                return false;
            }
            int deltaWidth = cell.width - width;
            int deltaHeight = cell.height - height;
            Cell first = new Cell(cell2);
            Cell second = new Cell(cell2);
            first.x = cell.x + width + this.mPadding;
            first.y = cell.y;
            first.width = deltaWidth - this.mPadding;
            second.x = cell.x;
            second.y = cell.y + height + this.mPadding;
            second.height = deltaHeight - this.mPadding;
            if (this.mSplitDecision.splitHorizontal(deltaWidth, deltaHeight, width, height)) {
                first.height = height;
                second.width = cell.width;
            } else {
                first.height = cell.height;
                second.width = width;
                second = first;
                first = second;
            }
            if (first.width > 0 && first.height > 0) {
                prev.next = first;
                prev = first;
            }
            if (second.width > 0 && second.height > 0) {
                prev.next = second;
                second.next = cell.next;
            } else {
                prev.next = cell.next;
            }
            cell.next = null;
            entry.x = cell.x;
            entry.y = cell.y;
            return true;
        }
    }
}
