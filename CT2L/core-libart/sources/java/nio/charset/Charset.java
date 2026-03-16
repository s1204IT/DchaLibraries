package java.nio.charset;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.spi.CharsetProvider;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import libcore.icu.NativeConverter;

public abstract class Charset implements Comparable<Charset> {
    private static final HashMap<String, Charset> CACHED_CHARSETS = new HashMap<>();
    private static final Charset DEFAULT_CHARSET = getDefaultCharset();
    private final HashSet<String> aliasesSet;
    private final String canonicalName;

    public abstract boolean contains(Charset charset);

    public abstract CharsetDecoder newDecoder();

    public abstract CharsetEncoder newEncoder();

    protected Charset(String canonicalName, String[] aliases) {
        checkCharsetName(canonicalName);
        this.canonicalName = canonicalName;
        this.aliasesSet = new HashSet<>();
        if (aliases != null) {
            for (String alias : aliases) {
                checkCharsetName(alias);
                this.aliasesSet.add(alias);
            }
        }
    }

    private static void checkCharsetName(String name) {
        if (name.isEmpty()) {
            throw new IllegalCharsetNameException(name);
        }
        if (!isValidCharsetNameStart(name.charAt(0))) {
            throw new IllegalCharsetNameException(name);
        }
        for (int i = 1; i < name.length(); i++) {
            if (!isValidCharsetNamePart(name.charAt(i))) {
                throw new IllegalCharsetNameException(name);
            }
        }
    }

    private static boolean isValidCharsetNameStart(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
    }

    private static boolean isValidCharsetNamePart(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || ((c >= '0' && c <= '9') || c == '-' || c == '.' || c == ':' || c == '_');
    }

    public static SortedMap<String, Charset> availableCharsets() {
        TreeMap<String, Charset> charsets = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        String[] arr$ = NativeConverter.getAvailableCharsetNames();
        for (String charsetName : arr$) {
            Charset charset = NativeConverter.charsetForName(charsetName);
            charsets.put(charset.name(), charset);
        }
        for (CharsetProvider charsetProvider : ServiceLoader.load(CharsetProvider.class)) {
            Iterator<Charset> it = charsetProvider.charsets();
            while (it.hasNext()) {
                Charset cs = it.next();
                if (!charsets.containsKey(cs.name())) {
                    charsets.put(cs.name(), cs);
                }
            }
        }
        return Collections.unmodifiableSortedMap(charsets);
    }

    private static Charset cacheCharset(String charsetName, Charset cs) {
        Charset canonicalCharset;
        synchronized (CACHED_CHARSETS) {
            String canonicalName = cs.name();
            canonicalCharset = CACHED_CHARSETS.get(canonicalName);
            if (canonicalCharset == null) {
                canonicalCharset = cs;
            }
            CACHED_CHARSETS.put(canonicalName, canonicalCharset);
            CACHED_CHARSETS.put(charsetName, canonicalCharset);
            for (String alias : cs.aliasesSet) {
                CACHED_CHARSETS.put(alias, canonicalCharset);
            }
        }
        return canonicalCharset;
    }

    public static Charset forName(String charsetName) {
        synchronized (CACHED_CHARSETS) {
            Charset cs = CACHED_CHARSETS.get(charsetName);
            if (cs != null) {
                return cs;
            }
            if (charsetName == null) {
                throw new IllegalCharsetNameException(null);
            }
            checkCharsetName(charsetName);
            Charset cs2 = NativeConverter.charsetForName(charsetName);
            if (cs2 != null) {
                return cacheCharset(charsetName, cs2);
            }
            for (CharsetProvider charsetProvider : ServiceLoader.load(CharsetProvider.class)) {
                Charset cs3 = charsetProvider.charsetForName(charsetName);
                if (cs3 != null) {
                    return cacheCharset(charsetName, cs3);
                }
            }
            throw new UnsupportedCharsetException(charsetName);
        }
    }

    public static Charset forNameUEE(String charsetName) throws UnsupportedEncodingException {
        try {
            return forName(charsetName);
        } catch (Exception cause) {
            UnsupportedEncodingException ex = new UnsupportedEncodingException(charsetName);
            ex.initCause(cause);
            throw ex;
        }
    }

    public static boolean isSupported(String charsetName) {
        try {
            forName(charsetName);
            return true;
        } catch (UnsupportedCharsetException e) {
            return false;
        }
    }

    public final String name() {
        return this.canonicalName;
    }

    public final Set<String> aliases() {
        return Collections.unmodifiableSet(this.aliasesSet);
    }

    public String displayName() {
        return this.canonicalName;
    }

    public String displayName(Locale l) {
        return this.canonicalName;
    }

    public final boolean isRegistered() {
        return (this.canonicalName.startsWith("x-") || this.canonicalName.startsWith("X-")) ? false : true;
    }

    public boolean canEncode() {
        return true;
    }

    public final ByteBuffer encode(CharBuffer buffer) {
        try {
            return newEncoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE).encode(buffer);
        } catch (CharacterCodingException ex) {
            throw new Error(ex.getMessage(), ex);
        }
    }

    public final ByteBuffer encode(String s) {
        return encode(CharBuffer.wrap(s));
    }

    public final CharBuffer decode(ByteBuffer buffer) {
        try {
            return newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE).decode(buffer);
        } catch (CharacterCodingException ex) {
            throw new Error(ex.getMessage(), ex);
        }
    }

    @Override
    public final int compareTo(Charset charset) {
        return this.canonicalName.compareToIgnoreCase(charset.canonicalName);
    }

    public final boolean equals(Object obj) {
        if (!(obj instanceof Charset)) {
            return false;
        }
        Charset that = (Charset) obj;
        return this.canonicalName.equals(that.canonicalName);
    }

    public final int hashCode() {
        return this.canonicalName.hashCode();
    }

    public final String toString() {
        return getClass().getName() + "[" + this.canonicalName + "]";
    }

    public static Charset defaultCharset() {
        return DEFAULT_CHARSET;
    }

    private static Charset getDefaultCharset() {
        String encoding = System.getProperty("file.encoding", "UTF-8");
        try {
            return forName(encoding);
        } catch (UnsupportedCharsetException e) {
            return forName("UTF-8");
        }
    }
}
