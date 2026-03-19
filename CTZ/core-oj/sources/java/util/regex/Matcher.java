package java.util.regex;

import dalvik.annotation.optimization.ReachabilitySensitive;
import libcore.util.NativeAllocationRegistry;

public final class Matcher implements MatchResult {
    private static final NativeAllocationRegistry registry = new NativeAllocationRegistry(Matcher.class.getClassLoader(), getNativeFinalizer(), nativeSize());

    @ReachabilitySensitive
    private long address;
    private boolean anchoringBounds = true;
    private int appendPos;
    private String input;
    private boolean matchFound;
    private int[] matchOffsets;
    private Runnable nativeFinalizer;
    private CharSequence originalInput;

    @ReachabilitySensitive
    private Pattern pattern;
    private int regionEnd;
    private int regionStart;
    private boolean transparentBounds;

    private static native boolean findImpl(long j, int i, int[] iArr);

    private static native boolean findNextImpl(long j, int[] iArr);

    private static native int getMatchedGroupIndex0(long j, String str);

    private static native long getNativeFinalizer();

    private static native int groupCountImpl(long j);

    private static native boolean hitEndImpl(long j);

    private static native boolean lookingAtImpl(long j, int[] iArr);

    private static native boolean matchesImpl(long j, int[] iArr);

    private static native int nativeSize();

    private static native long openImpl(long j);

    private static native boolean requireEndImpl(long j);

    private static native void setInputImpl(long j, String str, int i, int i2);

    private static native void useAnchoringBoundsImpl(long j, boolean z);

    private static native void useTransparentBoundsImpl(long j, boolean z);

    Matcher(Pattern pattern, CharSequence charSequence) {
        usePattern(pattern);
        reset(charSequence);
    }

    public Pattern pattern() {
        return this.pattern;
    }

    public MatchResult toMatchResult() {
        ensureMatch();
        return new OffsetBasedMatchResult(this.input, this.matchOffsets);
    }

    public Matcher usePattern(Pattern pattern) {
        if (pattern == null) {
            throw new IllegalArgumentException("newPattern == null");
        }
        this.pattern = pattern;
        synchronized (this) {
            if (this.nativeFinalizer != null) {
                this.nativeFinalizer.run();
                this.address = 0L;
                this.nativeFinalizer = null;
            }
            this.address = openImpl(this.pattern.address);
            this.nativeFinalizer = registry.registerNativeAllocation(this, this.address);
        }
        if (this.input != null) {
            resetForInput();
        }
        this.matchOffsets = new int[(groupCount() + 1) * 2];
        this.matchFound = false;
        return this;
    }

    @Override
    public int end() {
        return end(0);
    }

    @Override
    public int end(int i) {
        ensureMatch();
        return this.matchOffsets[(i * 2) + 1];
    }

    public int end(String str) {
        ensureMatch();
        return this.matchOffsets[(getMatchedGroupIndex(this.pattern.address, str) * 2) + 1];
    }

    @Override
    public String group() {
        return group(0);
    }

    @Override
    public String group(int i) {
        ensureMatch();
        int i2 = i * 2;
        int i3 = this.matchOffsets[i2];
        int i4 = this.matchOffsets[i2 + 1];
        if (i3 == -1 || i4 == -1) {
            return null;
        }
        return this.input.substring(i3, i4);
    }

    public String group(String str) {
        ensureMatch();
        int matchedGroupIndex = getMatchedGroupIndex(this.pattern.address, str) * 2;
        int i = this.matchOffsets[matchedGroupIndex];
        int i2 = this.matchOffsets[matchedGroupIndex + 1];
        if (i == -1 || i2 == -1) {
            return null;
        }
        return this.input.substring(i, i2);
    }

    @Override
    public int groupCount() {
        int iGroupCountImpl;
        synchronized (this) {
            iGroupCountImpl = groupCountImpl(this.address);
        }
        return iGroupCountImpl;
    }

    public boolean matches() {
        synchronized (this) {
            this.matchFound = matchesImpl(this.address, this.matchOffsets);
        }
        return this.matchFound;
    }

    public boolean find() {
        synchronized (this) {
            this.matchFound = findNextImpl(this.address, this.matchOffsets);
        }
        return this.matchFound;
    }

    public boolean find(int i) {
        reset();
        if (i < 0 || i > this.input.length()) {
            throw new IndexOutOfBoundsException("start=" + i + "; length=" + this.input.length());
        }
        synchronized (this) {
            this.matchFound = findImpl(this.address, i, this.matchOffsets);
        }
        return this.matchFound;
    }

    public boolean lookingAt() {
        synchronized (this) {
            this.matchFound = lookingAtImpl(this.address, this.matchOffsets);
        }
        return this.matchFound;
    }

    public static String quoteReplacement(String str) {
        if (str.indexOf(92) == -1 && str.indexOf(36) == -1) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt == '\\' || cCharAt == '$') {
                sb.append('\\');
            }
            sb.append(cCharAt);
        }
        return sb.toString();
    }

    public Matcher appendReplacement(StringBuffer stringBuffer, String str) {
        stringBuffer.append(this.input.substring(this.appendPos, start()));
        appendEvaluated(stringBuffer, str);
        this.appendPos = end();
        return this;
    }

    private void appendEvaluated(StringBuffer stringBuffer, String str) {
        boolean z = false;
        boolean z2 = false;
        boolean z3 = false;
        int i = -1;
        for (int i2 = 0; i2 < str.length(); i2++) {
            char cCharAt = str.charAt(i2);
            if (cCharAt == '\\' && !z2) {
                z2 = true;
            } else if (cCharAt == '$' && !z2) {
                z3 = true;
            } else if (cCharAt >= '0' && cCharAt <= '9' && z3) {
                stringBuffer.append(group(cCharAt - '0'));
                z3 = false;
            } else if (cCharAt == '{' && z3) {
                i = i2;
                z = true;
            } else if (cCharAt == '}' && z3 && z) {
                stringBuffer.append(group(str.substring(i + 1, i2)));
                z = false;
                z3 = false;
            } else if (cCharAt == '}' || !z3 || !z) {
                stringBuffer.append(cCharAt);
                z = false;
                z2 = false;
                z3 = false;
            }
        }
        if (z) {
            throw new IllegalArgumentException("Missing ending brace '}' from replacement string");
        }
        if (z2) {
            throw new ArrayIndexOutOfBoundsException(str.length());
        }
    }

    public StringBuffer appendTail(StringBuffer stringBuffer) {
        if (this.appendPos < this.regionEnd) {
            stringBuffer.append(this.input.substring(this.appendPos, this.regionEnd));
        }
        return stringBuffer;
    }

    public String replaceAll(String str) {
        reset();
        StringBuffer stringBuffer = new StringBuffer(this.input.length());
        while (find()) {
            appendReplacement(stringBuffer, str);
        }
        return appendTail(stringBuffer).toString();
    }

    public String replaceFirst(String str) {
        reset();
        StringBuffer stringBuffer = new StringBuffer(this.input.length());
        if (find()) {
            appendReplacement(stringBuffer, str);
        }
        return appendTail(stringBuffer).toString();
    }

    public Matcher region(int i, int i2) {
        return reset(this.originalInput, i, i2);
    }

    public int regionStart() {
        return this.regionStart;
    }

    public int regionEnd() {
        return this.regionEnd;
    }

    public boolean hasTransparentBounds() {
        return this.transparentBounds;
    }

    public Matcher useTransparentBounds(boolean z) {
        synchronized (this) {
            this.transparentBounds = z;
            useTransparentBoundsImpl(this.address, z);
        }
        return this;
    }

    public boolean hasAnchoringBounds() {
        return this.anchoringBounds;
    }

    public Matcher useAnchoringBounds(boolean z) {
        synchronized (this) {
            this.anchoringBounds = z;
            useAnchoringBoundsImpl(this.address, z);
        }
        return this;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("java.util.regex.Matcher");
        sb.append("[pattern=" + ((Object) pattern()));
        sb.append(" region=");
        sb.append(regionStart() + "," + regionEnd());
        sb.append(" lastmatch=");
        if (this.matchFound && group() != null) {
            sb.append(group());
        }
        sb.append("]");
        return sb.toString();
    }

    public boolean hitEnd() {
        boolean zHitEndImpl;
        synchronized (this) {
            zHitEndImpl = hitEndImpl(this.address);
        }
        return zHitEndImpl;
    }

    public boolean requireEnd() {
        boolean zRequireEndImpl;
        synchronized (this) {
            zRequireEndImpl = requireEndImpl(this.address);
        }
        return zRequireEndImpl;
    }

    public Matcher reset() {
        return reset(this.originalInput, 0, this.originalInput.length());
    }

    public Matcher reset(CharSequence charSequence) {
        return reset(charSequence, 0, charSequence.length());
    }

    private Matcher reset(CharSequence charSequence, int i, int i2) {
        if (charSequence == null) {
            throw new IllegalArgumentException("input == null");
        }
        if (i < 0 || i2 < 0 || i > charSequence.length() || i2 > charSequence.length() || i > i2) {
            throw new IndexOutOfBoundsException();
        }
        this.originalInput = charSequence;
        this.input = charSequence.toString();
        this.regionStart = i;
        this.regionEnd = i2;
        resetForInput();
        this.matchFound = false;
        this.appendPos = 0;
        return this;
    }

    private void resetForInput() {
        synchronized (this) {
            setInputImpl(this.address, this.input, this.regionStart, this.regionEnd);
            useAnchoringBoundsImpl(this.address, this.anchoringBounds);
            useTransparentBoundsImpl(this.address, this.transparentBounds);
        }
    }

    private void ensureMatch() {
        if (!this.matchFound) {
            throw new IllegalStateException("No successful match so far");
        }
    }

    @Override
    public int start() {
        return start(0);
    }

    @Override
    public int start(int i) throws IllegalStateException {
        ensureMatch();
        return this.matchOffsets[i * 2];
    }

    public int start(String str) {
        ensureMatch();
        return this.matchOffsets[getMatchedGroupIndex(this.pattern.address, str) * 2];
    }

    private static int getMatchedGroupIndex(long j, String str) {
        int matchedGroupIndex0 = getMatchedGroupIndex0(j, str);
        if (matchedGroupIndex0 < 0) {
            throw new IllegalArgumentException("No capturing group in the pattern with the name " + str);
        }
        return matchedGroupIndex0;
    }

    static final class OffsetBasedMatchResult implements MatchResult {
        private final String input;
        private final int[] offsets;

        OffsetBasedMatchResult(String str, int[] iArr) {
            this.input = str;
            this.offsets = (int[]) iArr.clone();
        }

        @Override
        public int start() {
            return start(0);
        }

        @Override
        public int start(int i) {
            return this.offsets[2 * i];
        }

        @Override
        public int end() {
            return end(0);
        }

        @Override
        public int end(int i) {
            return this.offsets[(2 * i) + 1];
        }

        @Override
        public String group() {
            return group(0);
        }

        @Override
        public String group(int i) {
            int iStart = start(i);
            int iEnd = end(i);
            if (iStart == -1 || iEnd == -1) {
                return null;
            }
            return this.input.substring(iStart, iEnd);
        }

        @Override
        public int groupCount() {
            return (this.offsets.length / 2) - 1;
        }
    }
}
