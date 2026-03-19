package android.icu.impl;

import android.icu.lang.UCharacter;
import android.icu.text.UTF16;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

public class TextTrieMap<V> {
    boolean _ignoreCase;
    private TextTrieMap<V>.Node _root = new Node();

    public interface ResultHandler<V> {
        boolean handlePrefixMatch(int i, Iterator<V> it);
    }

    public TextTrieMap(boolean z) {
        this._ignoreCase = z;
    }

    public TextTrieMap<V> put(CharSequence charSequence, V v) {
        this._root.add(new CharIterator(charSequence, 0, this._ignoreCase), v);
        return this;
    }

    public Iterator<V> get(String str) {
        return get(str, 0);
    }

    public Iterator<V> get(CharSequence charSequence, int i) {
        return get(charSequence, i, null);
    }

    public Iterator<V> get(CharSequence charSequence, int i, int[] iArr) {
        LongestMatchHandler longestMatchHandler = new LongestMatchHandler();
        find(charSequence, i, longestMatchHandler);
        if (iArr != null && iArr.length > 0) {
            iArr[0] = longestMatchHandler.getMatchLength();
        }
        return longestMatchHandler.getMatches();
    }

    public void find(CharSequence charSequence, ResultHandler<V> resultHandler) {
        find(charSequence, 0, resultHandler);
    }

    public void find(CharSequence charSequence, int i, ResultHandler<V> resultHandler) {
        find(this._root, new CharIterator(charSequence, i, this._ignoreCase), resultHandler);
    }

    private synchronized void find(TextTrieMap<V>.Node node, CharIterator charIterator, ResultHandler<V> resultHandler) {
        Iterator<V> itValues = node.values();
        if (itValues == null || resultHandler.handlePrefixMatch(charIterator.processedLength(), itValues)) {
            TextTrieMap<V>.Node nodeFindMatch = node.findMatch(charIterator);
            if (nodeFindMatch != null) {
                find(nodeFindMatch, charIterator, resultHandler);
            }
        }
    }

    public TextTrieMap<V>.ParseState openParseState(int i) {
        if (this._ignoreCase) {
            i = UCharacter.foldCase(i, true);
        }
        if (!this._root.hasChildFor(Character.charCount(i) == 1 ? (char) i : UTF16.getLeadSurrogate(i))) {
            return null;
        }
        return new ParseState(this._root);
    }

    public class ParseState {
        static final boolean $assertionsDisabled = false;
        private TextTrieMap<V>.Node node;
        private int offset = 0;
        private TextTrieMap<V>.Node.StepResult result;

        ParseState(TextTrieMap<V>.Node node) {
            this.node = node;
            Objects.requireNonNull(node);
            this.result = new Node.StepResult();
        }

        public void accept(int i) {
            if (TextTrieMap.this._ignoreCase) {
                i = UCharacter.foldCase(i, true);
            }
            int iCharCount = Character.charCount(i);
            this.node.takeStep(iCharCount == 1 ? (char) i : UTF16.getLeadSurrogate(i), this.offset, this.result);
            if (iCharCount == 2 && this.result.node != null) {
                this.result.node.takeStep(UTF16.getTrailSurrogate(i), this.result.offset, this.result);
            }
            this.node = this.result.node;
            this.offset = this.result.offset;
        }

        public Iterator<V> getCurrentMatches() {
            if (this.node != null && this.offset == this.node.charCount()) {
                return this.node.values();
            }
            return null;
        }

        public boolean atEnd() {
            return this.node == null || (this.node.charCount() == this.offset && ((Node) this.node)._children == null);
        }
    }

    public static class CharIterator implements Iterator<Character> {
        private boolean _ignoreCase;
        private int _nextIdx;
        private Character _remainingChar;
        private int _startIdx;
        private CharSequence _text;

        CharIterator(CharSequence charSequence, int i, boolean z) {
            this._text = charSequence;
            this._startIdx = i;
            this._nextIdx = i;
            this._ignoreCase = z;
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
                Character ch = this._remainingChar;
                this._remainingChar = null;
                return ch;
            }
            if (this._ignoreCase) {
                int iFoldCase = UCharacter.foldCase(Character.codePointAt(this._text, this._nextIdx), true);
                this._nextIdx += Character.charCount(iFoldCase);
                char[] chars = Character.toChars(iFoldCase);
                Character chValueOf = Character.valueOf(chars[0]);
                if (chars.length == 2) {
                    this._remainingChar = Character.valueOf(chars[1]);
                }
                return chValueOf;
            }
            Character chValueOf2 = Character.valueOf(this._text.charAt(this._nextIdx));
            this._nextIdx++;
            return chValueOf2;
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

        private LongestMatchHandler() {
            this.matches = null;
            this.length = 0;
        }

        @Override
        public boolean handlePrefixMatch(int i, Iterator<V> it) {
            if (i > this.length) {
                this.length = i;
                this.matches = it;
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
        static final boolean $assertionsDisabled = false;
        private List<TextTrieMap<V>.Node> _children;
        private char[] _text;
        private List<V> _values;

        private Node() {
        }

        private Node(char[] cArr, List<V> list, List<TextTrieMap<V>.Node> list2) {
            this._text = cArr;
            this._values = list;
            this._children = list2;
        }

        public int charCount() {
            if (this._text == null) {
                return 0;
            }
            return this._text.length;
        }

        public boolean hasChildFor(char c) {
            for (int i = 0; this._children != null && i < this._children.size(); i++) {
                TextTrieMap<V>.Node node = this._children.get(i);
                if (c < node._text[0]) {
                    break;
                }
                if (c == node._text[0]) {
                    return true;
                }
            }
            return false;
        }

        public Iterator<V> values() {
            if (this._values == null) {
                return null;
            }
            return this._values.iterator();
        }

        public void add(CharIterator charIterator, V v) {
            StringBuilder sb = new StringBuilder();
            while (charIterator.hasNext()) {
                sb.append(charIterator.next());
            }
            add(TextTrieMap.toCharArray(sb), 0, v);
        }

        public TextTrieMap<V>.Node findMatch(CharIterator charIterator) {
            if (this._children == null || !charIterator.hasNext()) {
                return null;
            }
            Character next = charIterator.next();
            for (TextTrieMap<V>.Node node : this._children) {
                if (next.charValue() < node._text[0]) {
                    return null;
                }
                if (next.charValue() == node._text[0]) {
                    if (node.matchFollowing(charIterator)) {
                        return node;
                    }
                    return null;
                }
            }
            return null;
        }

        public class StepResult {
            public TextTrieMap<V>.Node node;
            public int offset;

            public StepResult() {
            }
        }

        public void takeStep(char c, int i, TextTrieMap<V>.Node.StepResult stepResult) {
            if (i == charCount()) {
                for (int i2 = 0; this._children != null && i2 < this._children.size(); i2++) {
                    TextTrieMap<V>.Node node = this._children.get(i2);
                    if (c < node._text[0]) {
                        break;
                    }
                    if (c == node._text[0]) {
                        stepResult.node = node;
                        stepResult.offset = 1;
                        return;
                    }
                }
            } else if (this._text[i] == c) {
                stepResult.node = this;
                stepResult.offset = i + 1;
                return;
            }
            stepResult.node = null;
            stepResult.offset = -1;
        }

        private void add(char[] cArr, int i, V v) {
            TextTrieMap<V>.Node next;
            if (cArr.length == i) {
                this._values = addValue(this._values, v);
                return;
            }
            if (this._children == null) {
                this._children = new LinkedList();
                this._children.add(new Node(TextTrieMap.subArray(cArr, i), addValue(null, v), null));
                return;
            }
            ListIterator<TextTrieMap<V>.Node> listIterator = this._children.listIterator();
            do {
                if (listIterator.hasNext()) {
                    next = listIterator.next();
                    if (cArr[i] < next._text[0]) {
                        listIterator.previous();
                    }
                }
                listIterator.add(new Node(TextTrieMap.subArray(cArr, i), addValue(null, v), null));
                return;
            } while (cArr[i] != next._text[0]);
            int iLenMatches = next.lenMatches(cArr, i);
            if (iLenMatches == next._text.length) {
                next.add(cArr, i + iLenMatches, v);
            } else {
                next.split(iLenMatches);
                next.add(cArr, i + iLenMatches, v);
            }
        }

        private boolean matchFollowing(CharIterator charIterator) {
            for (int i = 1; i < this._text.length; i++) {
                if (!charIterator.hasNext() || charIterator.next().charValue() != this._text[i]) {
                    return false;
                }
            }
            return true;
        }

        private int lenMatches(char[] cArr, int i) {
            int length = cArr.length - i;
            if (this._text.length < length) {
                length = this._text.length;
            }
            int i2 = 0;
            while (i2 < length && this._text[i2] == cArr[i + i2]) {
                i2++;
            }
            return i2;
        }

        private void split(int i) {
            char[] cArrSubArray = TextTrieMap.subArray(this._text, i);
            this._text = TextTrieMap.subArray(this._text, 0, i);
            TextTrieMap<V>.Node node = new Node(cArrSubArray, this._values, this._children);
            this._values = null;
            this._children = new LinkedList();
            this._children.add(node);
        }

        private List<V> addValue(List<V> list, V v) {
            if (list == null) {
                list = new LinkedList<>();
            }
            list.add(v);
            return list;
        }
    }

    private static char[] toCharArray(CharSequence charSequence) {
        char[] cArr = new char[charSequence.length()];
        for (int i = 0; i < cArr.length; i++) {
            cArr[i] = charSequence.charAt(i);
        }
        return cArr;
    }

    private static char[] subArray(char[] cArr, int i) {
        if (i == 0) {
            return cArr;
        }
        char[] cArr2 = new char[cArr.length - i];
        System.arraycopy(cArr, i, cArr2, 0, cArr2.length);
        return cArr2;
    }

    private static char[] subArray(char[] cArr, int i, int i2) {
        if (i == 0 && i2 == cArr.length) {
            return cArr;
        }
        int i3 = i2 - i;
        char[] cArr2 = new char[i3];
        System.arraycopy(cArr, i, cArr2, 0, i3);
        return cArr2;
    }
}
