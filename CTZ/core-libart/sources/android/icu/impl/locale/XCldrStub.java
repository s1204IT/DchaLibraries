package android.icu.impl.locale;

import android.icu.util.ICUException;
import android.icu.util.ICUUncheckedIOException;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XCldrStub {

    public interface Predicate<T> {
        boolean test(T t);
    }

    public static class Multimap<K, V> {
        private final Map<K, Set<V>> map;
        private final Class<Set<V>> setClass;

        private Multimap(Map<K, Set<V>> map, Class cls) {
            this.map = map;
            this.setClass = (Class<Set<V>>) (cls == null ? HashSet.class : cls);
        }

        public Multimap<K, V> putAll(K k, V... vArr) {
            if (vArr.length != 0) {
                createSetIfMissing(k).addAll(Arrays.asList(vArr));
            }
            return this;
        }

        public void putAll(K k, Collection<V> collection) {
            if (!collection.isEmpty()) {
                createSetIfMissing(k).addAll(collection);
            }
        }

        public void putAll(Collection<K> collection, V v) {
            Iterator<K> it = collection.iterator();
            while (it.hasNext()) {
                put(it.next(), v);
            }
        }

        public void putAll(Multimap<K, V> multimap) {
            for (Map.Entry<K, Set<V>> entry : multimap.map.entrySet()) {
                putAll(entry.getKey(), entry.getValue());
            }
        }

        public void put(K k, V v) {
            createSetIfMissing(k).add(v);
        }

        private Set<V> createSetIfMissing(K k) {
            Set<V> set = this.map.get(k);
            if (set != null) {
                return set;
            }
            Map<K, Set<V>> map = this.map;
            Set<V> multimap = getInstance();
            map.put(k, multimap);
            return multimap;
        }

        private Set<V> getInstance() {
            try {
                return this.setClass.newInstance();
            } catch (Exception e) {
                throw new ICUException(e);
            }
        }

        public Set<V> get(K k) {
            return this.map.get(k);
        }

        public Set<K> keySet() {
            return this.map.keySet();
        }

        public Map<K, Set<V>> asMap() {
            return this.map;
        }

        public Set<V> values() {
            Collection<Set<V>> collectionValues = this.map.values();
            if (collectionValues.size() == 0) {
                return Collections.emptySet();
            }
            Set<V> multimap = getInstance();
            Iterator<Set<V>> it = collectionValues.iterator();
            while (it.hasNext()) {
                multimap.addAll(it.next());
            }
            return multimap;
        }

        public int size() {
            return this.map.size();
        }

        public Iterable<Map.Entry<K, V>> entries() {
            return new MultimapIterator(this.map);
        }

        public boolean equals(Object obj) {
            return this == obj || (obj != null && obj.getClass() == getClass() && this.map.equals(((Multimap) obj).map));
        }

        public int hashCode() {
            return this.map.hashCode();
        }
    }

    public static class Multimaps {
        public static <K, V, R extends Multimap<K, V>> R invertFrom(Multimap<V, K> multimap, R r) {
            for (Map.Entry<V, Set<K>> entry : multimap.asMap().entrySet()) {
                r.putAll(entry.getValue(), entry.getKey());
            }
            return r;
        }

        public static <K, V, R extends Multimap<K, V>> R invertFrom(Map<V, K> map, R r) {
            for (Map.Entry<V, K> entry : map.entrySet()) {
                r.put(entry.getValue(), entry.getKey());
            }
            return r;
        }

        public static <K, V> Map<K, V> forMap(Map<K, V> map) {
            return map;
        }
    }

    private static class MultimapIterator<K, V> implements Iterator<Map.Entry<K, V>>, Iterable<Map.Entry<K, V>> {
        private final ReusableEntry<K, V> entry;
        private final Iterator<Map.Entry<K, Set<V>>> it1;
        private Iterator<V> it2;

        private MultimapIterator(Map<K, Set<V>> map) {
            this.it2 = null;
            this.entry = new ReusableEntry<>();
            this.it1 = map.entrySet().iterator();
        }

        @Override
        public boolean hasNext() {
            return this.it1.hasNext() || (this.it2 != null && this.it2.hasNext());
        }

        @Override
        public Map.Entry<K, V> next() {
            if (this.it2 != null && this.it2.hasNext()) {
                this.entry.value = this.it2.next();
            } else {
                Map.Entry<K, Set<V>> next = this.it1.next();
                this.entry.key = next.getKey();
                this.it2 = next.getValue().iterator();
            }
            return this.entry;
        }

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return this;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private static class ReusableEntry<K, V> implements Map.Entry<K, V> {
        K key;
        V value;

        private ReusableEntry() {
        }

        @Override
        public K getKey() {
            return this.key;
        }

        @Override
        public V getValue() {
            return this.value;
        }

        @Override
        public V setValue(V v) {
            throw new UnsupportedOperationException();
        }
    }

    public static class HashMultimap<K, V> extends Multimap<K, V> {
        private HashMultimap() {
            super(new HashMap(), HashSet.class);
        }

        public static <K, V> HashMultimap<K, V> create() {
            return new HashMultimap<>();
        }
    }

    public static class TreeMultimap<K, V> extends Multimap<K, V> {
        private TreeMultimap() {
            super(new TreeMap(), TreeSet.class);
        }

        public static <K, V> TreeMultimap<K, V> create() {
            return new TreeMultimap<>();
        }
    }

    public static class LinkedHashMultimap<K, V> extends Multimap<K, V> {
        private LinkedHashMultimap() {
            super(new LinkedHashMap(), LinkedHashSet.class);
        }

        public static <K, V> LinkedHashMultimap<K, V> create() {
            return new LinkedHashMultimap<>();
        }
    }

    public static <T> String join(T[] tArr, String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tArr.length; i++) {
            if (i != 0) {
                sb.append(str);
            }
            sb.append(tArr[i]);
        }
        return sb.toString();
    }

    public static <T> String join(Iterable<T> iterable, String str) {
        StringBuilder sb = new StringBuilder();
        boolean z = true;
        for (T t : iterable) {
            if (z) {
                z = false;
            } else {
                sb.append(str);
            }
            sb.append(t.toString());
        }
        return sb.toString();
    }

    public static class CollectionUtilities {
        public static <T, U extends Iterable<T>> String join(U u, String str) {
            return XCldrStub.join(u, str);
        }
    }

    public static class Joiner {
        private final String separator;

        private Joiner(String str) {
            this.separator = str;
        }

        public static final Joiner on(String str) {
            return new Joiner(str);
        }

        public <T> String join(T[] tArr) {
            return XCldrStub.join(tArr, this.separator);
        }

        public <T> String join(Iterable<T> iterable) {
            return XCldrStub.join(iterable, this.separator);
        }
    }

    public static class Splitter {
        Pattern pattern;
        boolean trimResults;

        public Splitter(char c) {
            this(Pattern.compile("\\Q" + c + "\\E"));
        }

        public Splitter(Pattern pattern) {
            this.trimResults = false;
            this.pattern = pattern;
        }

        public static Splitter on(char c) {
            return new Splitter(c);
        }

        public static Splitter on(Pattern pattern) {
            return new Splitter(pattern);
        }

        public List<String> splitToList(String str) {
            String[] strArrSplit = this.pattern.split(str);
            if (this.trimResults) {
                for (int i = 0; i < strArrSplit.length; i++) {
                    strArrSplit[i] = strArrSplit[i].trim();
                }
            }
            return Arrays.asList(strArrSplit);
        }

        public Splitter trimResults() {
            this.trimResults = true;
            return this;
        }

        public Iterable<String> split(String str) {
            return splitToList(str);
        }
    }

    public static class ImmutableSet {
        public static <T> Set<T> copyOf(Set<T> set) {
            return Collections.unmodifiableSet(new LinkedHashSet(set));
        }
    }

    public static class ImmutableMap {
        public static <K, V> Map<K, V> copyOf(Map<K, V> map) {
            return Collections.unmodifiableMap(new LinkedHashMap(map));
        }
    }

    public static class ImmutableMultimap {
        public static <K, V> Multimap<K, V> copyOf(Multimap<K, V> multimap) {
            Set setUnmodifiableSet;
            LinkedHashMap linkedHashMap = new LinkedHashMap();
            for (Map.Entry<K, Set<V>> entry : multimap.asMap().entrySet()) {
                Set<V> value = entry.getValue();
                K key = entry.getKey();
                if (value.size() == 1) {
                    setUnmodifiableSet = Collections.singleton(value.iterator().next());
                } else {
                    setUnmodifiableSet = Collections.unmodifiableSet(new LinkedHashSet(value));
                }
                linkedHashMap.put(key, setUnmodifiableSet);
            }
            return new Multimap<>(Collections.unmodifiableMap(linkedHashMap), null);
        }
    }

    public static class FileUtilities {
        public static final Charset UTF8 = Charset.forName("utf-8");

        public static BufferedReader openFile(Class<?> cls, String str) {
            return openFile(cls, str, UTF8);
        }

        public static BufferedReader openFile(Class<?> cls, String str, Charset charset) {
            try {
                InputStream resourceAsStream = cls.getResourceAsStream(str);
                if (charset == null) {
                    charset = UTF8;
                }
                return new BufferedReader(new InputStreamReader(resourceAsStream, charset), 65536);
            } catch (Exception e) {
                String canonicalName = cls == null ? null : cls.getCanonicalName();
                try {
                    throw new ICUUncheckedIOException("Couldn't open file " + str + "; in path " + new File(getRelativeFileName(cls, "../util/")).getCanonicalPath() + "; relative to class: " + canonicalName, e);
                } catch (Exception e2) {
                    throw new ICUUncheckedIOException("Couldn't open file: " + str + "; relative to class: " + canonicalName, e);
                }
            }
        }

        public static String getRelativeFileName(Class<?> cls, String str) {
            if (cls == null) {
                cls = FileUtilities.class;
            }
            String string = cls.getResource(str).toString();
            if (string.startsWith("file:")) {
                return string.substring(5);
            }
            if (string.startsWith("jar:file:")) {
                return string.substring(9);
            }
            throw new ICUUncheckedIOException("File not found: " + string);
        }
    }

    public static class RegexUtilities {
        public static int findMismatch(Matcher matcher, CharSequence charSequence) {
            int i = 1;
            while (i < charSequence.length() && (matcher.reset(charSequence.subSequence(0, i)).matches() || matcher.hitEnd())) {
                i++;
            }
            return i - 1;
        }

        public static String showMismatch(Matcher matcher, CharSequence charSequence) {
            int iFindMismatch = findMismatch(matcher, charSequence);
            return ((Object) charSequence.subSequence(0, iFindMismatch)) + "☹" + ((Object) charSequence.subSequence(iFindMismatch, charSequence.length()));
        }
    }
}
