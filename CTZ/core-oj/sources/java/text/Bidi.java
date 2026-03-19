package java.text;

public final class Bidi {
    public static final int DIRECTION_DEFAULT_LEFT_TO_RIGHT = -2;
    public static final int DIRECTION_DEFAULT_RIGHT_TO_LEFT = -1;
    public static final int DIRECTION_LEFT_TO_RIGHT = 0;
    public static final int DIRECTION_RIGHT_TO_LEFT = 1;
    private final android.icu.text.Bidi bidiBase;

    private static int translateConstToIcu(int i) {
        switch (i) {
        }
        return 0;
    }

    public Bidi(String str, int i) {
        if (str == null) {
            throw new IllegalArgumentException("paragraph is null");
        }
        this.bidiBase = new android.icu.text.Bidi(str.toCharArray(), 0, null, 0, str.length(), translateConstToIcu(i));
    }

    public Bidi(AttributedCharacterIterator attributedCharacterIterator) {
        if (attributedCharacterIterator == null) {
            throw new IllegalArgumentException("paragraph is null");
        }
        this.bidiBase = new android.icu.text.Bidi(attributedCharacterIterator);
    }

    public Bidi(char[] cArr, int i, byte[] bArr, int i2, int i3, int i4) {
        if (cArr == null) {
            throw new IllegalArgumentException("text is null");
        }
        if (i3 < 0) {
            throw new IllegalArgumentException("bad length: " + i3);
        }
        if (i < 0 || i3 > cArr.length - i) {
            throw new IllegalArgumentException("bad range: " + i + " length: " + i3 + " for text of length: " + cArr.length);
        }
        if (bArr != null && (i2 < 0 || i3 > bArr.length - i2)) {
            throw new IllegalArgumentException("bad range: " + i2 + " length: " + i3 + " for embeddings of length: " + cArr.length);
        }
        this.bidiBase = new android.icu.text.Bidi(cArr, i, bArr, i2, i3, translateConstToIcu(i4));
    }

    private Bidi(android.icu.text.Bidi bidi) {
        this.bidiBase = bidi;
    }

    public Bidi createLineBidi(int i, int i2) {
        if (i < 0 || i2 < 0 || i > i2 || i2 > getLength()) {
            throw new IllegalArgumentException("Invalid ranges (start=" + i + ", limit=" + i2 + ", length=" + getLength() + ")");
        }
        if (i == i2) {
            return new Bidi(new android.icu.text.Bidi(new char[0], 0, new byte[0], 0, 0, translateConstToIcu(0)));
        }
        return new Bidi(this.bidiBase.createLineBidi(i, i2));
    }

    public boolean isMixed() {
        return this.bidiBase.isMixed();
    }

    public boolean isLeftToRight() {
        return this.bidiBase.isLeftToRight();
    }

    public boolean isRightToLeft() {
        return this.bidiBase.isRightToLeft();
    }

    public int getLength() {
        return this.bidiBase.getLength();
    }

    public boolean baseIsLeftToRight() {
        return this.bidiBase.baseIsLeftToRight();
    }

    public int getBaseLevel() {
        return this.bidiBase.getParaLevel();
    }

    public int getLevelAt(int i) {
        try {
            return this.bidiBase.getLevelAt(i);
        } catch (IllegalArgumentException e) {
            return getBaseLevel();
        }
    }

    public int getRunCount() {
        int iCountRuns = this.bidiBase.countRuns();
        if (iCountRuns == 0) {
            return 1;
        }
        return iCountRuns;
    }

    public int getRunLevel(int i) {
        if (i == getRunCount()) {
            return getBaseLevel();
        }
        return this.bidiBase.countRuns() == 0 ? this.bidiBase.getBaseLevel() : this.bidiBase.getRunLevel(i);
    }

    public int getRunStart(int i) {
        if (i == getRunCount()) {
            return getBaseLevel();
        }
        if (this.bidiBase.countRuns() == 0) {
            return 0;
        }
        return this.bidiBase.getRunStart(i);
    }

    public int getRunLimit(int i) {
        if (i == getRunCount()) {
            return getBaseLevel();
        }
        return this.bidiBase.countRuns() == 0 ? this.bidiBase.getLength() : this.bidiBase.getRunLimit(i);
    }

    public static boolean requiresBidi(char[] cArr, int i, int i2) {
        if (i < 0 || i > i2 || i2 > cArr.length) {
            throw new IllegalArgumentException("Value start " + i + " is out of range 0 to " + i2);
        }
        return android.icu.text.Bidi.requiresBidi(cArr, i, i2);
    }

    public static void reorderVisually(byte[] bArr, int i, Object[] objArr, int i2, int i3) {
        if (i < 0 || bArr.length <= i) {
            StringBuilder sb = new StringBuilder();
            sb.append("Value levelStart ");
            sb.append(i);
            sb.append(" is out of range 0 to ");
            sb.append(bArr.length - 1);
            throw new IllegalArgumentException(sb.toString());
        }
        if (i2 < 0 || objArr.length <= i2) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Value objectStart ");
            sb2.append(i);
            sb2.append(" is out of range 0 to ");
            sb2.append(objArr.length - 1);
            throw new IllegalArgumentException(sb2.toString());
        }
        if (i3 < 0 || objArr.length < i2 + i3) {
            throw new IllegalArgumentException("Value count " + i + " is out of range 0 to " + (objArr.length - i2));
        }
        android.icu.text.Bidi.reorderVisually(bArr, i, objArr, i2, i3);
    }

    public String toString() {
        return getClass().getName() + "[direction: " + ((int) this.bidiBase.getDirection()) + " baseLevel: " + this.bidiBase.getBaseLevel() + " length: " + this.bidiBase.getLength() + " runs: " + this.bidiBase.getRunCount() + "]";
    }
}
