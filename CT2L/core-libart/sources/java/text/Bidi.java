package java.text;

import java.awt.font.NumericShaper;
import java.awt.font.TextAttribute;
import java.util.ArrayList;
import java.util.Arrays;

public final class Bidi {
    public static final int DIRECTION_DEFAULT_LEFT_TO_RIGHT = -2;
    public static final int DIRECTION_DEFAULT_RIGHT_TO_LEFT = -1;
    public static final int DIRECTION_LEFT_TO_RIGHT = 0;
    public static final int DIRECTION_RIGHT_TO_LEFT = 1;
    private static final int UBIDI_LEVEL_OVERRIDE = 128;
    private static final int UBiDiDirection_UBIDI_LTR = 0;
    private static final int UBiDiDirection_UBIDI_MIXED = 2;
    private static final int UBiDiDirection_UBIDI_RTL = 1;
    private int baseLevel;
    private int direction;
    private int length;
    private byte[] offsetLevel;
    private Run[] runs;
    private boolean unidirectional;

    private static native void ubidi_close(long j);

    private static native int ubidi_countRuns(long j);

    private static native int ubidi_getDirection(long j);

    private static native int ubidi_getLength(long j);

    private static native byte[] ubidi_getLevels(long j);

    private static native byte ubidi_getParaLevel(long j);

    private static native Run[] ubidi_getRuns(long j);

    private static native long ubidi_open();

    private static native int[] ubidi_reorderVisual(byte[] bArr, int i);

    private static native long ubidi_setLine(long j, int i, int i2);

    private static native void ubidi_setPara(long j, char[] cArr, int i, int i2, byte[] bArr);

    static class Run {
        private final int level;
        private final int limit;
        private final int start;

        public Run(int start, int limit, int level) {
            this.start = start;
            this.limit = limit;
            this.level = level;
        }

        public int getLevel() {
            return this.level;
        }

        public int getLimit() {
            return this.limit;
        }

        public int getStart() {
            return this.start;
        }
    }

    public Bidi(AttributedCharacterIterator paragraph) {
        if (paragraph == null) {
            throw new IllegalArgumentException("paragraph is null");
        }
        int begin = paragraph.getBeginIndex();
        int end = paragraph.getEndIndex();
        int length = end - begin;
        char[] text = new char[length + 1];
        if (length != 0) {
            text[0] = paragraph.first();
        } else {
            paragraph.first();
        }
        int flags = -2;
        Object direction = paragraph.getAttribute(TextAttribute.RUN_DIRECTION);
        if (direction != null && (direction instanceof Boolean)) {
            flags = direction.equals(TextAttribute.RUN_DIRECTION_LTR) ? 0 : 1;
        }
        byte[] embeddings = null;
        int textLimit = 1;
        int i = 1;
        while (i < length) {
            Object embedding = paragraph.getAttribute(TextAttribute.BIDI_EMBEDDING);
            if (embedding != null && (embedding instanceof Integer)) {
                int embLevel = ((Integer) embedding).intValue();
                embeddings = embeddings == null ? new byte[length] : embeddings;
                while (i < textLimit) {
                    text[i] = paragraph.next();
                    embeddings[i - 1] = (byte) embLevel;
                    i++;
                }
            } else {
                while (i < textLimit) {
                    text[i] = paragraph.next();
                    i++;
                }
            }
            textLimit = (paragraph.getRunLimit(TextAttribute.BIDI_EMBEDDING) - begin) + 1;
        }
        Object numericShaper = paragraph.getAttribute(TextAttribute.NUMERIC_SHAPING);
        if (numericShaper != null && (numericShaper instanceof NumericShaper)) {
            ((NumericShaper) numericShaper).shape(text, 0, length);
        }
        long bidi = 0;
        try {
            bidi = createUBiDi(text, 0, embeddings, 0, length, flags);
            readBidiInfo(bidi);
        } finally {
            ubidi_close(bidi);
        }
    }

    public Bidi(char[] text, int textStart, byte[] embeddings, int embStart, int paragraphLength, int flags) {
        if (text == null || text.length - textStart < paragraphLength) {
            throw new IllegalArgumentException();
        }
        if (embeddings != null && embeddings.length - embStart < paragraphLength) {
            throw new IllegalArgumentException();
        }
        if (textStart < 0) {
            throw new IllegalArgumentException("Negative textStart value " + textStart);
        }
        if (embStart < 0) {
            throw new IllegalArgumentException("Negative embStart value " + embStart);
        }
        if (paragraphLength < 0) {
            throw new IllegalArgumentException("Negative paragraph length " + paragraphLength);
        }
        long bidi = 0;
        try {
            bidi = createUBiDi(text, textStart, embeddings, embStart, paragraphLength, flags);
            readBidiInfo(bidi);
        } finally {
            ubidi_close(bidi);
        }
    }

    public Bidi(String paragraph, int flags) {
        this(paragraph == null ? null : paragraph.toCharArray(), 0, null, 0, paragraph == null ? 0 : paragraph.length(), flags);
    }

    private static long createUBiDi(char[] text, int textStart, byte[] embeddings, int embStart, int paragraphLength, int flags) {
        byte[] realEmbeddings = null;
        if (text == null || text.length - textStart < paragraphLength) {
            throw new IllegalArgumentException();
        }
        char[] realText = new char[paragraphLength];
        System.arraycopy(text, textStart, realText, 0, paragraphLength);
        if (embeddings != null) {
            if (embeddings.length - embStart < paragraphLength) {
                throw new IllegalArgumentException();
            }
            if (paragraphLength > 0) {
                Bidi temp = new Bidi(text, textStart, null, 0, paragraphLength, flags);
                realEmbeddings = new byte[paragraphLength];
                System.arraycopy(temp.offsetLevel, 0, realEmbeddings, 0, paragraphLength);
                for (int i = 0; i < paragraphLength; i++) {
                    byte e = embeddings[i];
                    if (e < 0) {
                        realEmbeddings[i] = (byte) (128 - e);
                    } else if (e > 0) {
                        realEmbeddings[i] = e;
                    } else {
                        realEmbeddings[i] = (byte) (realEmbeddings[i] | Byte.MIN_VALUE);
                    }
                }
            }
        }
        if (flags > 1 || flags < -2) {
            flags = 0;
        }
        long bidi = 0;
        boolean needsDeletion = true;
        try {
            bidi = ubidi_open();
            ubidi_setPara(bidi, realText, paragraphLength, flags, realEmbeddings);
            needsDeletion = false;
            return bidi;
        } finally {
            if (needsDeletion) {
                ubidi_close(bidi);
            }
        }
    }

    private Bidi(long pBidi) {
        readBidiInfo(pBidi);
    }

    private void readBidiInfo(long pBidi) {
        this.length = ubidi_getLength(pBidi);
        this.offsetLevel = this.length == 0 ? null : ubidi_getLevels(pBidi);
        this.baseLevel = ubidi_getParaLevel(pBidi);
        int runCount = ubidi_countRuns(pBidi);
        if (runCount == 0) {
            this.unidirectional = true;
            this.runs = null;
        } else if (runCount < 0) {
            this.runs = null;
        } else {
            this.runs = ubidi_getRuns(pBidi);
            if (runCount == 1 && this.runs[0].getLevel() == this.baseLevel) {
                this.unidirectional = true;
                this.runs = null;
            }
        }
        this.direction = ubidi_getDirection(pBidi);
    }

    public boolean baseIsLeftToRight() {
        return this.baseLevel % 2 == 0;
    }

    public Bidi createLineBidi(int lineStart, int lineLimit) {
        Bidi bidi;
        if (lineStart < 0 || lineLimit < 0 || lineLimit > this.length || lineStart > lineLimit) {
            throw new IllegalArgumentException("Invalid ranges (start=" + lineStart + ", limit=" + lineLimit + ", length=" + this.length + ")");
        }
        char[] text = new char[this.length];
        Arrays.fill(text, 'a');
        byte[] embeddings = new byte[this.length];
        for (int i = 0; i < embeddings.length; i++) {
            embeddings[i] = (byte) (-this.offsetLevel[i]);
        }
        int dir = baseIsLeftToRight() ? 0 : 1;
        long parent = 0;
        try {
            parent = createUBiDi(text, 0, embeddings, 0, this.length, dir);
            if (lineStart == lineLimit) {
                bidi = createEmptyLineBidi(parent);
            } else {
                bidi = new Bidi(ubidi_setLine(parent, lineStart, lineLimit));
            }
            return bidi;
        } finally {
            ubidi_close(parent);
        }
    }

    private Bidi createEmptyLineBidi(long parent) {
        Bidi result = new Bidi(parent);
        result.length = 0;
        result.offsetLevel = null;
        result.runs = null;
        result.unidirectional = true;
        return result;
    }

    public int getBaseLevel() {
        return this.baseLevel;
    }

    public int getLength() {
        return this.length;
    }

    public int getLevelAt(int offset) {
        try {
            return this.offsetLevel[offset] & Byte.MAX_VALUE;
        } catch (RuntimeException e) {
            return this.baseLevel;
        }
    }

    public int getRunCount() {
        if (this.unidirectional) {
            return 1;
        }
        return this.runs.length;
    }

    public int getRunLevel(int run) {
        return this.unidirectional ? this.baseLevel : this.runs[run].getLevel();
    }

    public int getRunLimit(int run) {
        return this.unidirectional ? this.length : this.runs[run].getLimit();
    }

    public int getRunStart(int run) {
        if (this.unidirectional) {
            return 0;
        }
        return this.runs[run].getStart();
    }

    public boolean isLeftToRight() {
        return this.direction == 0;
    }

    public boolean isMixed() {
        return this.direction == 2;
    }

    public boolean isRightToLeft() {
        return this.direction == 1;
    }

    public static void reorderVisually(byte[] levels, int levelStart, Object[] objects, int objectStart, int count) {
        if (count < 0 || levelStart < 0 || objectStart < 0 || count > levels.length - levelStart || count > objects.length - objectStart) {
            throw new IllegalArgumentException("Invalid ranges (levels=" + levels.length + ", levelStart=" + levelStart + ", objects=" + objects.length + ", objectStart=" + objectStart + ", count=" + count + ")");
        }
        byte[] realLevels = new byte[count];
        System.arraycopy(levels, levelStart, realLevels, 0, count);
        int[] indices = ubidi_reorderVisual(realLevels, count);
        ArrayList<Object> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(objects[indices[i] + objectStart]);
        }
        System.arraycopy(result.toArray(), 0, objects, objectStart, count);
    }

    public static boolean requiresBidi(char[] text, int start, int limit) {
        if (limit < 0 || start < 0 || start > limit || limit > text.length) {
            throw new IllegalArgumentException();
        }
        Bidi bidi = new Bidi(text, start, null, 0, limit - start, 0);
        return !bidi.isLeftToRight();
    }

    public String toString() {
        return getClass().getName() + "[direction: " + this.direction + " baseLevel: " + this.baseLevel + " length: " + this.length + " runs: " + Arrays.toString(this.runs) + "]";
    }
}
