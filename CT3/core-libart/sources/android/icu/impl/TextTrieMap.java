package android.icu.impl;

import android.icu.lang.UCharacter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class TextTrieMap<V> {
    boolean _ignoreCase;
    private TextTrieMap<V>.Node _root = new Node(this, null);

    public interface ResultHandler<V> {
        boolean handlePrefixMatch(int i, Iterator<V> it);
    }

    public TextTrieMap(boolean ignoreCase) {
        this._ignoreCase = ignoreCase;
    }

    public TextTrieMap<V> put(CharSequence text, V val) {
        CharIterator chitr = new CharIterator(text, 0, this._ignoreCase);
        this._root.add(chitr, val);
        return this;
    }

    public Iterator<V> get(String text) {
        return get(text, 0);
    }

    public Iterator<V> get(CharSequence text, int start) {
        return get(text, start, null);
    }

    public Iterator<V> get(CharSequence text, int start, int[] matchLen) {
        LongestMatchHandler<V> handler = new LongestMatchHandler<>(null);
        find(text, start, handler);
        if (matchLen != null && matchLen.length > 0) {
            matchLen[0] = handler.getMatchLength();
        }
        return handler.getMatches();
    }

    public void find(CharSequence text, ResultHandler<V> handler) {
        find(text, 0, handler);
    }

    public void find(CharSequence text, int offset, ResultHandler<V> handler) {
        CharIterator chitr = new CharIterator(text, offset, this._ignoreCase);
        find(this._root, chitr, handler);
    }

    private synchronized void find(TextTrieMap<V>.Node node, CharIterator chitr, ResultHandler<V> handler) {
        Iterator<V> values = node.values();
        if (values != null && !handler.handlePrefixMatch(chitr.processedLength(), values)) {
            return;
        }
        TextTrieMap<V>.Node nextMatch = node.findMatch(chitr);
        if (nextMatch != null) {
            find(nextMatch, chitr, handler);
        }
    }

    public static class CharIterator implements Iterator<Character> {
        private boolean _ignoreCase;
        private int _nextIdx;
        private Character _remainingChar;
        private int _startIdx;
        private CharSequence _text;

        CharIterator(CharSequence text, int offset, boolean ignoreCase) {
            this._text = text;
            this._startIdx = offset;
            this._nextIdx = offset;
            this._ignoreCase = ignoreCase;
        }

        @Override
        public boolean hasNext() {
            if (this._nextIdx == this._text.length() && this._remainingChar == null) {
                return false;
            }
            return true;
        }

        @Override
        public Character next() {
            if (this._nextIdx == this._text.length() && this._remainingChar == null) {
                return null;
            }
            if (this._remainingChar != null) {
                Character next = this._remainingChar;
                this._remainingChar = null;
                return next;
            }
            if (this._ignoreCase) {
                int cp = UCharacter.foldCase(Character.codePointAt(this._text, this._nextIdx), true);
                this._nextIdx += Character.charCount(cp);
                char[] chars = Character.toChars(cp);
                Character next2 = Character.valueOf(chars[0]);
                if (chars.length == 2) {
                    this._remainingChar = Character.valueOf(chars[1]);
                    return next2;
                }
                return next2;
            }
            Character next3 = Character.valueOf(this._text.charAt(this._nextIdx));
            this._nextIdx++;
            return next3;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove() not supproted");
        }

        public int nextIndex() {
            return this._nextIdx;
        }

        public int processedLength() {
            if (this._remainingChar != null) {
                throw new IllegalStateException("In the middle of surrogate pair");
            }
            return this._nextIdx - this._startIdx;
        }
    }

    private static class LongestMatchHandler<V> implements ResultHandler<V> {
        private int length;
        private Iterator<V> matches;

        LongestMatchHandler(LongestMatchHandler longestMatchHandler) {
            this();
        }

        private LongestMatchHandler() {
            this.matches = null;
            this.length = 0;
        }

        @Override
        public boolean handlePrefixMatch(int matchLength, Iterator<V> values) {
            if (matchLength > this.length) {
                this.length = matchLength;
                this.matches = values;
                return true;
            }
            return true;
        }

        public Iterator<V> getMatches() {
            return this.matches;
        }

        public int getMatchLength() {
            return this.length;
        }
    }

    private class Node {
        private List<TextTrieMap<V>.Node> _children;
        private char[] _text;
        private List<V> _values;

        Node(TextTrieMap this$0, Node node) {
            this();
        }

        private Node() {
        }

        private Node(char[] text, List<V> values, List<TextTrieMap<V>.Node> children) {
            this._text = text;
            this._values = values;
            this._children = children;
        }

        public Iterator<V> values() {
            if (this._values == null) {
                return null;
            }
            return this._values.iterator();
        }

        public void add(CharIterator chitr, V value) {
            StringBuilder buf = new StringBuilder();
            while (chitr.hasNext()) {
                buf.append(chitr.next());
            }
            add(TextTrieMap.toCharArray(buf), 0, value);
        }

        public TextTrieMap<V>.Node findMatch(CharIterator chitr) {
            if (this._children == null || !chitr.hasNext()) {
                return null;
            }
            Character ch = chitr.next();
            for (TextTrieMap<V>.Node child : this._children) {
                if (ch.charValue() < child._text[0]) {
                    return null;
                }
                if (ch.charValue() == child._text[0]) {
                    if (!child.matchFollowing(chitr)) {
                        return null;
                    }
                    return child;
                }
            }
            return null;
        }

        private void add(char[] text, int offset, V value) {
            TextTrieMap<V>.Node next;
            if (text.length == offset) {
                this._values = addValue(this._values, value);
                return;
            }
            if (this._children == null) {
                this._children = new LinkedList();
                TextTrieMap<V>.Node child = new Node(TextTrieMap.subArray(text, offset), addValue(null, value), null);
                this._children.add(child);
                return;
            }
            ListIterator<TextTrieMap<V>.Node> litr = this._children.listIterator();
            do {
                if (litr.hasNext()) {
                    next = litr.next();
                    if (text[offset] < next._text[0]) {
                        litr.previous();
                    }
                }
                litr.add(new Node(TextTrieMap.subArray(text, offset), addValue(null, value), null));
                return;
            } while (text[offset] != next._text[0]);
            int matchLen = next.lenMatches(text, offset);
            if (matchLen == next._text.length) {
                next.add(text, offset + matchLen, value);
            } else {
                next.split(matchLen);
                next.add(text, offset + matchLen, value);
            }
        }

        private boolean matchFollowing(CharIterator chitr) {
            for (int idx = 1; idx < this._text.length; idx++) {
                if (!chitr.hasNext()) {
                    return false;
                }
                Character ch = chitr.next();
                if (ch.charValue() != this._text[idx]) {
                    return false;
                }
            }
            return true;
        }

        private int lenMatches(char[] text, int offset) {
            int textLen = text.length - offset;
            int limit = this._text.length < textLen ? this._text.length : textLen;
            int len = 0;
            while (len < limit && this._text[len] == text[offset + len]) {
                len++;
            }
            return len;
        }

        private void split(int offset) {
            char[] childText = TextTrieMap.subArray(this._text, offset);
            this._text = TextTrieMap.subArray(this._text, 0, offset);
            TextTrieMap<V>.Node child = new Node(childText, this._values, this._children);
            this._values = null;
            this._children = new LinkedList();
            this._children.add(child);
        }

        private List<V> addValue(List<V> list, V value) {
            if (list == null) {
                list = new LinkedList<>();
            }
            list.add(value);
            return list;
        }
    }

    private static char[] toCharArray(CharSequence text) {
        char[] array = new char[text.length()];
        for (int i = 0; i < array.length; i++) {
            array[i] = text.charAt(i);
        }
        return array;
    }

    private static char[] subArray(char[] array, int start) {
        if (start == 0) {
            return array;
        }
        char[] sub = new char[array.length - start];
        System.arraycopy(array, start, sub, 0, sub.length);
        return sub;
    }

    private static char[] subArray(char[] array, int start, int limit) {
        if (start == 0 && limit == array.length) {
            return array;
        }
        char[] sub = new char[limit - start];
        System.arraycopy(array, start, sub, 0, limit - start);
        return sub;
    }
}
