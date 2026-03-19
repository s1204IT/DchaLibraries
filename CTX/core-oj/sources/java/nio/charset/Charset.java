package java.nio.charset;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.spi.CharsetProvider;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import libcore.icu.NativeConverter;
import sun.misc.ASCIICaseInsensitiveComparator;
import sun.misc.VM;
import sun.nio.cs.ThreadLocalCoders;
import sun.security.action.GetPropertyAction;

public abstract class Charset implements Comparable<Charset> {
    private static Charset defaultCharset;
    private Set<String> aliasSet = null;
    private final String[] aliases;
    private final String name;
    private static volatile String bugLevel = null;
    private static volatile Map.Entry<String, Charset> cache1 = null;
    private static final HashMap<String, Charset> cache2 = new HashMap<>();
    private static ThreadLocal<ThreadLocal<?>> gate = new ThreadLocal<>();

    public abstract boolean contains(Charset charset);

    public abstract CharsetDecoder newDecoder();

    public abstract CharsetEncoder newEncoder();

    static boolean atBugLevel(String str) {
        String str2 = bugLevel;
        if (str2 == null) {
            if (!VM.isBooted()) {
                return false;
            }
            str2 = (String) AccessController.doPrivileged(new GetPropertyAction("sun.nio.cs.bugLevel", ""));
            bugLevel = str2;
        }
        return str2.equals(str);
    }

    private static void checkName(String str) {
        int length = str.length();
        if (!atBugLevel("1.4") && length == 0) {
            throw new IllegalCharsetNameException(str);
        }
        for (int i = 0; i < length; i++) {
            char cCharAt = str.charAt(i);
            if ((cCharAt < 'A' || cCharAt > 'Z') && ((cCharAt < 'a' || cCharAt > 'z') && ((cCharAt < '0' || cCharAt > '9') && ((cCharAt != '-' || i == 0) && ((cCharAt != '+' || i == 0) && ((cCharAt != ':' || i == 0) && ((cCharAt != '_' || i == 0) && (cCharAt != '.' || i == 0)))))))) {
                throw new IllegalCharsetNameException(str);
            }
        }
    }

    private static void cache(String str, Charset charset) {
        synchronized (cache2) {
            String strName = charset.name();
            Charset charset2 = cache2.get(strName);
            if (charset2 == null) {
                cache2.put(strName, charset);
                Iterator<String> it = charset.aliases().iterator();
                while (it.hasNext()) {
                    cache2.put(it.next(), charset);
                }
            } else {
                charset = charset2;
            }
            cache2.put(str, charset);
        }
        cache1 = new AbstractMap.SimpleImmutableEntry(str, charset);
    }

    private static Iterator<CharsetProvider> providers() {
        return new Iterator<CharsetProvider>() {
            ServiceLoader<CharsetProvider> sl = ServiceLoader.load(CharsetProvider.class);
            Iterator<CharsetProvider> i = this.sl.iterator();
            CharsetProvider next = null;

            private boolean getNext() {
                while (this.next == null) {
                    try {
                    } catch (ServiceConfigurationError e) {
                        if (!(e.getCause() instanceof SecurityException)) {
                            throw e;
                        }
                    }
                    if (!this.i.hasNext()) {
                        return false;
                    }
                    this.next = this.i.next();
                }
                return true;
            }

            @Override
            public boolean hasNext() {
                return getNext();
            }

            @Override
            public CharsetProvider next() {
                if (!getNext()) {
                    throw new NoSuchElementException();
                }
                CharsetProvider charsetProvider = this.next;
                this.next = null;
                return charsetProvider;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static Charset lookupViaProviders(final String str) {
        if (!VM.isBooted() || gate.get() != null) {
            return null;
        }
        try {
            gate.set(gate);
            return (Charset) AccessController.doPrivileged(new PrivilegedAction<Charset>() {
                @Override
                public Charset run() {
                    Iterator itProviders = Charset.providers();
                    while (itProviders.hasNext()) {
                        Charset charsetCharsetForName = ((CharsetProvider) itProviders.next()).charsetForName(str);
                        if (charsetCharsetForName != null) {
                            return charsetCharsetForName;
                        }
                    }
                    return null;
                }
            });
        } finally {
            gate.set(null);
        }
    }

    private static Charset lookup(String str) {
        if (str == null) {
            throw new IllegalArgumentException("Null charset name");
        }
        Map.Entry<String, Charset> entry = cache1;
        if (entry != null && str.equals(entry.getKey())) {
            return entry.getValue();
        }
        return lookup2(str);
    }

    private static Charset lookup2(String str) {
        synchronized (cache2) {
            Charset charset = cache2.get(str);
            if (charset != null) {
                cache1 = new AbstractMap.SimpleImmutableEntry(str, charset);
                return charset;
            }
            Charset charsetCharsetForName = NativeConverter.charsetForName(str);
            if (charsetCharsetForName != null || (charsetCharsetForName = lookupViaProviders(str)) != null) {
                cache(str, charsetCharsetForName);
                return charsetCharsetForName;
            }
            checkName(str);
            return null;
        }
    }

    public static boolean isSupported(String str) {
        return lookup(str) != null;
    }

    public static Charset forName(String str) {
        Charset charsetLookup = lookup(str);
        if (charsetLookup != null) {
            return charsetLookup;
        }
        throw new UnsupportedCharsetException(str);
    }

    public static Charset forNameUEE(String str) throws UnsupportedEncodingException {
        try {
            return forName(str);
        } catch (Exception e) {
            UnsupportedEncodingException unsupportedEncodingException = new UnsupportedEncodingException(str);
            unsupportedEncodingException.initCause(e);
            throw unsupportedEncodingException;
        }
    }

    private static void put(Iterator<Charset> it, Map<String, Charset> map) {
        while (it.hasNext()) {
            Charset next = it.next();
            if (!map.containsKey(next.name())) {
                map.put(next.name(), next);
            }
        }
    }

    public static SortedMap<String, Charset> availableCharsets() {
        return (SortedMap) AccessController.doPrivileged(new PrivilegedAction<SortedMap<String, Charset>>() {
            @Override
            public SortedMap<String, Charset> run() {
                TreeMap treeMap = new TreeMap(ASCIICaseInsensitiveComparator.CASE_INSENSITIVE_ORDER);
                for (String str : NativeConverter.getAvailableCharsetNames()) {
                    Charset charsetCharsetForName = NativeConverter.charsetForName(str);
                    treeMap.put(charsetCharsetForName.name(), charsetCharsetForName);
                }
                Iterator itProviders = Charset.providers();
                while (itProviders.hasNext()) {
                    Charset.put(((CharsetProvider) itProviders.next()).charsets(), treeMap);
                }
                return Collections.unmodifiableSortedMap(treeMap);
            }
        });
    }

    public static Charset defaultCharset() {
        Charset charset;
        synchronized (Charset.class) {
            if (defaultCharset == null) {
                defaultCharset = StandardCharsets.UTF_8;
            }
            charset = defaultCharset;
        }
        return charset;
    }

    protected Charset(String str, String[] strArr) {
        checkName(str);
        strArr = strArr == null ? new String[0] : strArr;
        for (String str2 : strArr) {
            checkName(str2);
        }
        this.name = str;
        this.aliases = strArr;
    }

    public final String name() {
        return this.name;
    }

    public final Set<String> aliases() {
        if (this.aliasSet != null) {
            return this.aliasSet;
        }
        int length = this.aliases.length;
        HashSet hashSet = new HashSet(length);
        for (int i = 0; i < length; i++) {
            hashSet.add(this.aliases[i]);
        }
        this.aliasSet = Collections.unmodifiableSet(hashSet);
        return this.aliasSet;
    }

    public String displayName() {
        return this.name;
    }

    public final boolean isRegistered() {
        return (this.name.startsWith("X-") || this.name.startsWith("x-")) ? false : true;
    }

    public String displayName(Locale locale) {
        return this.name;
    }

    public boolean canEncode() {
        return true;
    }

    public final CharBuffer decode(ByteBuffer byteBuffer) {
        try {
            return ThreadLocalCoders.decoderFor(this).onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE).decode(byteBuffer);
        } catch (CharacterCodingException e) {
            throw new Error(e);
        }
    }

    public final ByteBuffer encode(CharBuffer charBuffer) {
        try {
            return ThreadLocalCoders.encoderFor(this).onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE).encode(charBuffer);
        } catch (CharacterCodingException e) {
            throw new Error(e);
        }
    }

    public final ByteBuffer encode(String str) {
        return encode(CharBuffer.wrap(str));
    }

    @Override
    public final int compareTo(Charset charset) {
        return name().compareToIgnoreCase(charset.name());
    }

    public final int hashCode() {
        return name().hashCode();
    }

    public final boolean equals(Object obj) {
        if (!(obj instanceof Charset)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        return this.name.equals(((Charset) obj).name());
    }

    public final String toString() {
        return name();
    }
}
