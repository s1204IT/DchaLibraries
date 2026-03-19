package java.util.regex;

import dalvik.annotation.optimization.ReachabilitySensitive;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import libcore.util.EmptyArray;
import libcore.util.NativeAllocationRegistry;

public final class Pattern implements Serializable {
    public static final int CANON_EQ = 128;
    public static final int CASE_INSENSITIVE = 2;
    public static final int COMMENTS = 4;
    public static final int DOTALL = 32;
    private static final String FASTSPLIT_METACHARACTERS = "\\?*+[](){}^$.|";
    public static final int LITERAL = 16;
    public static final int MULTILINE = 8;
    public static final int UNICODE_CASE = 64;
    public static final int UNICODE_CHARACTER_CLASS = 256;
    public static final int UNIX_LINES = 1;
    private static final NativeAllocationRegistry registry = new NativeAllocationRegistry(Pattern.class.getClassLoader(), getNativeFinalizer(), nativeSize());
    private static final long serialVersionUID = 5073258162644648461L;

    @ReachabilitySensitive
    transient long address;
    private final int flags;
    private final String pattern;

    private static native long compileImpl(String str, int i);

    private static native long getNativeFinalizer();

    private static native int nativeSize();

    public static Pattern compile(String str) {
        return new Pattern(str, 0);
    }

    public static Pattern compile(String str, int i) throws PatternSyntaxException {
        return new Pattern(str, i);
    }

    public String pattern() {
        return this.pattern;
    }

    public String toString() {
        return this.pattern;
    }

    public Matcher matcher(CharSequence charSequence) {
        return new Matcher(this, charSequence);
    }

    public int flags() {
        return this.flags;
    }

    public static boolean matches(String str, CharSequence charSequence) {
        return compile(str).matcher(charSequence).matches();
    }

    public String[] split(CharSequence charSequence, int i) {
        int i2;
        String[] strArrFastSplit = fastSplit(this.pattern, charSequence.toString(), i);
        if (strArrFastSplit != null) {
            return strArrFastSplit;
        }
        boolean z = i > 0;
        ArrayList arrayList = new ArrayList();
        Matcher matcher = matcher(charSequence);
        int iEnd = 0;
        while (matcher.find()) {
            if (!z || arrayList.size() < i - 1) {
                arrayList.add(charSequence.subSequence(iEnd, matcher.start()).toString());
                iEnd = matcher.end();
            } else if (arrayList.size() == i2) {
                arrayList.add(charSequence.subSequence(iEnd, charSequence.length()).toString());
                iEnd = matcher.end();
            }
        }
        if (iEnd == 0) {
            return new String[]{charSequence.toString()};
        }
        if (!z || arrayList.size() < i) {
            arrayList.add(charSequence.subSequence(iEnd, charSequence.length()).toString());
        }
        int size = arrayList.size();
        if (i == 0) {
            while (size > 0 && ((String) arrayList.get(size - 1)).equals("")) {
                size--;
            }
        }
        return (String[]) arrayList.subList(0, size).toArray(new String[size]);
    }

    public static String[] fastSplit(String str, String str2, int i) {
        int iIndexOf;
        int length = str.length();
        if (length == 0) {
            return null;
        }
        char cCharAt = str.charAt(0);
        if (length != 1 || FASTSPLIT_METACHARACTERS.indexOf(cCharAt) != -1) {
            if (length != 2 || cCharAt != '\\') {
                return null;
            }
            cCharAt = str.charAt(1);
            if (FASTSPLIT_METACHARACTERS.indexOf(cCharAt) == -1) {
                return null;
            }
        }
        if (str2.isEmpty()) {
            return new String[]{""};
        }
        int length2 = 0;
        int i2 = 0;
        while (true) {
            int i3 = length2 + 1;
            if (i3 == i || (iIndexOf = str2.indexOf(cCharAt, i2)) == -1) {
                break;
            }
            i2 = iIndexOf + 1;
            length2 = i3;
        }
        int length3 = str2.length();
        if (i == 0 && i2 == length3) {
            if (length2 == length3) {
                return EmptyArray.STRING;
            }
            do {
                i2--;
            } while (str2.charAt(i2 - 1) == cCharAt);
            length2 -= str2.length() - i2;
        } else {
            i2 = length3;
        }
        String[] strArr = new String[length2 + 1];
        int i4 = 0;
        for (int i5 = 0; i5 != length2; i5++) {
            int iIndexOf2 = str2.indexOf(cCharAt, i4);
            strArr[i5] = str2.substring(i4, iIndexOf2);
            i4 = iIndexOf2 + 1;
        }
        strArr[length2] = str2.substring(i4, i2);
        return strArr;
    }

    public String[] split(CharSequence charSequence) {
        return split(charSequence, 0);
    }

    public static String quote(String str) {
        if (str.indexOf("\\E") == -1) {
            return "\\Q" + str + "\\E";
        }
        StringBuilder sb = new StringBuilder(str.length() * 2);
        sb.append("\\Q");
        int i = 0;
        while (true) {
            int iIndexOf = str.indexOf("\\E", i);
            if (iIndexOf != -1) {
                sb.append(str.substring(i, iIndexOf));
                i = iIndexOf + 2;
                sb.append("\\E\\\\E\\Q");
            } else {
                sb.append(str.substring(i, str.length()));
                sb.append("\\E");
                return sb.toString();
            }
        }
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        compile();
    }

    private Pattern(String str, int i) {
        if ((i & 128) != 0) {
            throw new UnsupportedOperationException("CANON_EQ flag not supported");
        }
        int i2 = i & (-128);
        if (i2 != 0) {
            throw new IllegalArgumentException("Unsupported flags: " + i2);
        }
        this.pattern = str;
        this.flags = i;
        compile();
    }

    private void compile() throws PatternSyntaxException {
        if (this.pattern == null) {
            throw new NullPointerException("pattern == null");
        }
        String strQuote = this.pattern;
        if ((this.flags & 16) != 0) {
            strQuote = quote(this.pattern);
        }
        this.address = compileImpl(strQuote, this.flags & 47);
        registry.registerNativeAllocation(this, this.address);
    }

    public Predicate<String> asPredicate() {
        return new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return this.f$0.matcher((String) obj).find();
            }
        };
    }

    public Stream<String> splitAsStream(final CharSequence charSequence) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<String>() {
            private int current;
            private int emptyElementCount;
            private final Matcher matcher;
            private String nextElement;

            {
                this.matcher = Pattern.this.matcher(charSequence);
            }

            @Override
            public String next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                if (this.emptyElementCount == 0) {
                    String str = this.nextElement;
                    this.nextElement = null;
                    return str;
                }
                this.emptyElementCount--;
                return "";
            }

            @Override
            public boolean hasNext() {
                if (this.nextElement != null || this.emptyElementCount > 0) {
                    return true;
                }
                if (this.current == charSequence.length()) {
                    return false;
                }
                while (this.matcher.find()) {
                    this.nextElement = charSequence.subSequence(this.current, this.matcher.start()).toString();
                    this.current = this.matcher.end();
                    if (!this.nextElement.isEmpty()) {
                        return true;
                    }
                    if (this.current > 0) {
                        this.emptyElementCount++;
                    }
                }
                this.nextElement = charSequence.subSequence(this.current, charSequence.length()).toString();
                this.current = charSequence.length();
                if (!this.nextElement.isEmpty()) {
                    return true;
                }
                this.emptyElementCount = 0;
                this.nextElement = null;
                return false;
            }
        }, 272), false);
    }
}
