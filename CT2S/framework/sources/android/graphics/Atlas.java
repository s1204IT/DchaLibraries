package android.graphics;

public class Atlas {
    public static final int FLAG_ADD_PADDING = 2;
    public static final int FLAG_ALLOW_ROTATIONS = 1;
    public static final int FLAG_DEFAULTS = 2;
    private final Policy mPolicy;

    public static class Entry {
        public boolean rotated;
        public int x;
        public int y;
    }

    public enum Type {
        SliceMinArea,
        SliceMaxArea,
        SliceShortAxis,
        SliceLongAxis
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
        return this.mPolicy.pack(width, height, entry);
    }

    private static Policy findPolicy(Type type, int width, int height, int flags) {
        switch (type) {
            case SliceMinArea:
                return new SlicePolicy(width, height, flags, new SlicePolicy.MinAreaSplitDecision());
            case SliceMaxArea:
                return new SlicePolicy(width, height, flags, new SlicePolicy.MaxAreaSplitDecision());
            case SliceShortAxis:
                return new SlicePolicy(width, height, flags, new SlicePolicy.ShorterFreeAxisSplitDecision());
            case SliceLongAxis:
                return new SlicePolicy(width, height, flags, new SlicePolicy.LongerFreeAxisSplitDecision());
            default:
                return null;
        }
    }

    private static abstract class Policy {
        abstract Entry pack(int i, int i2, Entry entry);

        private Policy() {
        }
    }

    private static class SlicePolicy extends Policy {
        private final boolean mAllowRotation;
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

            private Cell() {
            }

            public String toString() {
                return String.format("cell[x=%d y=%d width=%d height=%d", Integer.valueOf(this.x), Integer.valueOf(this.y), Integer.valueOf(this.width), Integer.valueOf(this.height));
            }
        }

        SlicePolicy(int width, int height, int flags, SplitDecision splitDecision) {
            super();
            this.mRoot = new Cell();
            this.mAllowRotation = (flags & 1) != 0;
            this.mPadding = (flags & 2) == 0 ? 0 : 1;
            Cell first = new Cell();
            int i = this.mPadding;
            first.y = i;
            first.x = i;
            first.width = width - (this.mPadding * 2);
            first.height = height - (this.mPadding * 2);
            this.mRoot.next = first;
            this.mSplitDecision = splitDecision;
        }

        @Override
        Entry pack(int width, int height, Entry entry) {
            Cell prev = this.mRoot;
            for (Cell cell = this.mRoot.next; cell != null; cell = cell.next) {
                if (!insert(cell, prev, width, height, entry)) {
                    prev = cell;
                } else {
                    return entry;
                }
            }
            return null;
        }

        private static class MinAreaSplitDecision implements SplitDecision {
            private MinAreaSplitDecision() {
            }

            @Override
            public boolean splitHorizontal(int freeWidth, int freeHeight, int rectWidth, int rectHeight) {
                return rectWidth * freeHeight > freeWidth * rectHeight;
            }
        }

        private static class MaxAreaSplitDecision implements SplitDecision {
            private MaxAreaSplitDecision() {
            }

            @Override
            public boolean splitHorizontal(int freeWidth, int freeHeight, int rectWidth, int rectHeight) {
                return rectWidth * freeHeight <= freeWidth * rectHeight;
            }
        }

        private static class ShorterFreeAxisSplitDecision implements SplitDecision {
            private ShorterFreeAxisSplitDecision() {
            }

            @Override
            public boolean splitHorizontal(int freeWidth, int freeHeight, int rectWidth, int rectHeight) {
                return freeWidth <= freeHeight;
            }
        }

        private static class LongerFreeAxisSplitDecision implements SplitDecision {
            private LongerFreeAxisSplitDecision() {
            }

            @Override
            public boolean splitHorizontal(int freeWidth, int freeHeight, int rectWidth, int rectHeight) {
                return freeWidth > freeHeight;
            }
        }

        private boolean insert(Cell cell, Cell prev, int width, int height, Entry entry) {
            boolean rotated = false;
            if (cell.width < width || cell.height < height) {
                if (!this.mAllowRotation || cell.width < height || cell.height < width) {
                    return false;
                }
                width = height;
                height = width;
                rotated = true;
            }
            int deltaWidth = cell.width - width;
            int deltaHeight = cell.height - height;
            Cell first = new Cell();
            Cell second = new Cell();
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
                first = second;
                second = first;
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
            entry.rotated = rotated;
            return true;
        }
    }
}
