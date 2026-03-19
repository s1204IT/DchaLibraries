package java.text;

import java.text.AttributedCharacterIterator;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class AttributedString {
    private static final int ARRAY_SIZE_INCREMENT = 10;
    int runArraySize;
    Vector<Object>[] runAttributeValues;
    Vector<AttributedCharacterIterator.Attribute>[] runAttributes;
    int runCount;
    int[] runStarts;
    String text;

    AttributedString(AttributedCharacterIterator[] attributedCharacterIteratorArr) {
        if (attributedCharacterIteratorArr == null) {
            throw new NullPointerException("Iterators must not be null");
        }
        if (attributedCharacterIteratorArr.length == 0) {
            this.text = "";
            return;
        }
        StringBuffer stringBuffer = new StringBuffer();
        int i = 0;
        for (AttributedCharacterIterator attributedCharacterIterator : attributedCharacterIteratorArr) {
            appendContents(stringBuffer, attributedCharacterIterator);
        }
        this.text = stringBuffer.toString();
        if (this.text.length() > 0) {
            Map<AttributedCharacterIterator.Attribute, Object> map = null;
            int i2 = 0;
            while (i < attributedCharacterIteratorArr.length) {
                AttributedCharacterIterator attributedCharacterIterator2 = attributedCharacterIteratorArr[i];
                int beginIndex = attributedCharacterIterator2.getBeginIndex();
                int endIndex = attributedCharacterIterator2.getEndIndex();
                Map<AttributedCharacterIterator.Attribute, Object> map2 = map;
                int runLimit = beginIndex;
                while (runLimit < endIndex) {
                    attributedCharacterIterator2.setIndex(runLimit);
                    Map<AttributedCharacterIterator.Attribute, Object> attributes = attributedCharacterIterator2.getAttributes();
                    if (mapsDiffer(map2, attributes)) {
                        setAttributes(attributes, (runLimit - beginIndex) + i2);
                    }
                    runLimit = attributedCharacterIterator2.getRunLimit();
                    map2 = attributes;
                }
                i2 += endIndex - beginIndex;
                i++;
                map = map2;
            }
        }
    }

    public AttributedString(String str) {
        if (str == null) {
            throw new NullPointerException();
        }
        this.text = str;
    }

    public AttributedString(String str, Map<? extends AttributedCharacterIterator.Attribute, ?> map) {
        if (str == null || map == null) {
            throw new NullPointerException();
        }
        this.text = str;
        if (str.length() == 0) {
            if (map.isEmpty()) {
                return;
            } else {
                throw new IllegalArgumentException("Can't add attribute to 0-length text");
            }
        }
        int size = map.size();
        if (size > 0) {
            createRunAttributeDataVectors();
            Vector<AttributedCharacterIterator.Attribute> vector = new Vector<>(size);
            Vector<Object> vector2 = new Vector<>(size);
            this.runAttributes[0] = vector;
            this.runAttributeValues[0] = vector2;
            for (Map.Entry<? extends AttributedCharacterIterator.Attribute, ?> entry : map.entrySet()) {
                vector.addElement(entry.getKey());
                vector2.addElement(entry.getValue());
            }
        }
    }

    public AttributedString(AttributedCharacterIterator attributedCharacterIterator) {
        this(attributedCharacterIterator, attributedCharacterIterator.getBeginIndex(), attributedCharacterIterator.getEndIndex(), null);
    }

    public AttributedString(AttributedCharacterIterator attributedCharacterIterator, int i, int i2) {
        this(attributedCharacterIterator, i, i2, null);
    }

    public AttributedString(AttributedCharacterIterator attributedCharacterIterator, int i, int i2, AttributedCharacterIterator.Attribute[] attributeArr) {
        if (attributedCharacterIterator == null) {
            throw new NullPointerException();
        }
        int beginIndex = attributedCharacterIterator.getBeginIndex();
        int endIndex = attributedCharacterIterator.getEndIndex();
        if (i < beginIndex || i2 > endIndex || i > i2) {
            throw new IllegalArgumentException("Invalid substring range");
        }
        StringBuffer stringBuffer = new StringBuffer();
        attributedCharacterIterator.setIndex(i);
        char cCurrent = attributedCharacterIterator.current();
        while (attributedCharacterIterator.getIndex() < i2) {
            stringBuffer.append(cCurrent);
            cCurrent = attributedCharacterIterator.next();
        }
        this.text = stringBuffer.toString();
        if (i == i2) {
            return;
        }
        HashSet<AttributedCharacterIterator.Attribute> hashSet = new HashSet();
        if (attributeArr == null) {
            hashSet.addAll(attributedCharacterIterator.getAllAttributeKeys());
        } else {
            for (AttributedCharacterIterator.Attribute attribute : attributeArr) {
                hashSet.add(attribute);
            }
            hashSet.retainAll(attributedCharacterIterator.getAllAttributeKeys());
        }
        if (hashSet.isEmpty()) {
            return;
        }
        for (AttributedCharacterIterator.Attribute attribute2 : hashSet) {
            attributedCharacterIterator.setIndex(beginIndex);
            while (attributedCharacterIterator.getIndex() < i2) {
                int runStart = attributedCharacterIterator.getRunStart(attribute2);
                int runLimit = attributedCharacterIterator.getRunLimit(attribute2);
                Object attribute3 = attributedCharacterIterator.getAttribute(attribute2);
                if (attribute3 != null) {
                    if (attribute3 instanceof Annotation) {
                        if (runStart >= i && runLimit <= i2) {
                            addAttribute(attribute2, attribute3, runStart - i, runLimit - i);
                        } else if (runLimit > i2) {
                            break;
                        }
                    } else {
                        if (runStart >= i2) {
                            break;
                        }
                        if (runLimit > i) {
                            runStart = runStart < i ? i : runStart;
                            runLimit = runLimit > i2 ? i2 : runLimit;
                            if (runStart != runLimit) {
                                addAttribute(attribute2, attribute3, runStart - i, runLimit - i);
                            }
                        }
                    }
                }
                attributedCharacterIterator.setIndex(runLimit);
            }
        }
    }

    public void addAttribute(AttributedCharacterIterator.Attribute attribute, Object obj) {
        if (attribute == null) {
            throw new NullPointerException();
        }
        int length = length();
        if (length == 0) {
            throw new IllegalArgumentException("Can't add attribute to 0-length text");
        }
        addAttributeImpl(attribute, obj, 0, length);
    }

    public void addAttribute(AttributedCharacterIterator.Attribute attribute, Object obj, int i, int i2) {
        if (attribute == null) {
            throw new NullPointerException();
        }
        if (i < 0 || i2 > length() || i >= i2) {
            throw new IllegalArgumentException("Invalid substring range");
        }
        addAttributeImpl(attribute, obj, i, i2);
    }

    public void addAttributes(Map<? extends AttributedCharacterIterator.Attribute, ?> map, int i, int i2) {
        if (map == null) {
            throw new NullPointerException();
        }
        if (i < 0 || i2 > length() || i > i2) {
            throw new IllegalArgumentException("Invalid substring range");
        }
        if (i == i2) {
            if (map.isEmpty()) {
                return;
            } else {
                throw new IllegalArgumentException("Can't add attribute to 0-length text");
            }
        }
        if (this.runCount == 0) {
            createRunAttributeDataVectors();
        }
        int iEnsureRunBreak = ensureRunBreak(i);
        int iEnsureRunBreak2 = ensureRunBreak(i2);
        for (Map.Entry<? extends AttributedCharacterIterator.Attribute, ?> entry : map.entrySet()) {
            addAttributeRunData(entry.getKey(), entry.getValue(), iEnsureRunBreak, iEnsureRunBreak2);
        }
    }

    private synchronized void addAttributeImpl(AttributedCharacterIterator.Attribute attribute, Object obj, int i, int i2) {
        if (this.runCount == 0) {
            createRunAttributeDataVectors();
        }
        addAttributeRunData(attribute, obj, ensureRunBreak(i), ensureRunBreak(i2));
    }

    private final void createRunAttributeDataVectors() {
        this.runStarts = new int[10];
        this.runAttributes = new Vector[10];
        this.runAttributeValues = new Vector[10];
        this.runArraySize = 10;
        this.runCount = 1;
    }

    private final int ensureRunBreak(int i) {
        return ensureRunBreak(i, true);
    }

    private final int ensureRunBreak(int i, boolean z) {
        Vector<AttributedCharacterIterator.Attribute> vector;
        if (i == length()) {
            return this.runCount;
        }
        int i2 = 0;
        while (i2 < this.runCount && this.runStarts[i2] < i) {
            i2++;
        }
        if (i2 < this.runCount && this.runStarts[i2] == i) {
            return i2;
        }
        if (this.runCount == this.runArraySize) {
            int i3 = this.runArraySize + 10;
            int[] iArr = new int[i3];
            Vector<AttributedCharacterIterator.Attribute>[] vectorArr = new Vector[i3];
            Vector<Object>[] vectorArr2 = new Vector[i3];
            for (int i4 = 0; i4 < this.runArraySize; i4++) {
                iArr[i4] = this.runStarts[i4];
                vectorArr[i4] = this.runAttributes[i4];
                vectorArr2[i4] = this.runAttributeValues[i4];
            }
            this.runStarts = iArr;
            this.runAttributes = vectorArr;
            this.runAttributeValues = vectorArr2;
            this.runArraySize = i3;
        }
        Vector<Object> vector2 = null;
        if (z) {
            int i5 = i2 - 1;
            Vector<AttributedCharacterIterator.Attribute> vector3 = this.runAttributes[i5];
            Vector<Object> vector4 = this.runAttributeValues[i5];
            if (vector3 != null) {
                vector = new Vector<>(vector3);
            } else {
                vector = null;
            }
            if (vector4 != null) {
                vector2 = new Vector<>(vector4);
            }
        } else {
            vector = null;
        }
        this.runCount++;
        for (int i6 = this.runCount - 1; i6 > i2; i6--) {
            int i7 = i6 - 1;
            this.runStarts[i6] = this.runStarts[i7];
            this.runAttributes[i6] = this.runAttributes[i7];
            this.runAttributeValues[i6] = this.runAttributeValues[i7];
        }
        this.runStarts[i2] = i;
        this.runAttributes[i2] = vector;
        this.runAttributeValues[i2] = vector2;
        return i2;
    }

    private void addAttributeRunData(AttributedCharacterIterator.Attribute attribute, Object obj, int i, int i2) {
        int iIndexOf;
        while (i < i2) {
            if (this.runAttributes[i] == null) {
                Vector<AttributedCharacterIterator.Attribute> vector = new Vector<>();
                Vector<Object> vector2 = new Vector<>();
                this.runAttributes[i] = vector;
                this.runAttributeValues[i] = vector2;
                iIndexOf = -1;
            } else {
                iIndexOf = this.runAttributes[i].indexOf(attribute);
            }
            if (iIndexOf == -1) {
                int size = this.runAttributes[i].size();
                this.runAttributes[i].addElement(attribute);
                try {
                    this.runAttributeValues[i].addElement(obj);
                } catch (Exception e) {
                    this.runAttributes[i].setSize(size);
                    this.runAttributeValues[i].setSize(size);
                }
            } else {
                this.runAttributeValues[i].set(iIndexOf, obj);
            }
            i++;
        }
    }

    public AttributedCharacterIterator getIterator() {
        return getIterator(null, 0, length());
    }

    public AttributedCharacterIterator getIterator(AttributedCharacterIterator.Attribute[] attributeArr) {
        return getIterator(attributeArr, 0, length());
    }

    public AttributedCharacterIterator getIterator(AttributedCharacterIterator.Attribute[] attributeArr, int i, int i2) {
        return new AttributedStringIterator(attributeArr, i, i2);
    }

    int length() {
        return this.text.length();
    }

    private char charAt(int i) {
        return this.text.charAt(i);
    }

    private synchronized Object getAttribute(AttributedCharacterIterator.Attribute attribute, int i) {
        Vector<AttributedCharacterIterator.Attribute> vector = this.runAttributes[i];
        Vector<Object> vector2 = this.runAttributeValues[i];
        if (vector == null) {
            return null;
        }
        int iIndexOf = vector.indexOf(attribute);
        if (iIndexOf == -1) {
            return null;
        }
        return vector2.elementAt(iIndexOf);
    }

    private Object getAttributeCheckRange(AttributedCharacterIterator.Attribute attribute, int i, int i2, int i3) {
        int i4;
        Object attribute2 = getAttribute(attribute, i);
        if (attribute2 instanceof Annotation) {
            if (i2 > 0) {
                int i5 = this.runStarts[i];
                int i6 = i;
                while (i5 >= i2 && valuesMatch(attribute2, getAttribute(attribute, i6 - 1))) {
                    i6--;
                    i5 = this.runStarts[i6];
                }
                if (i5 < i2) {
                    return null;
                }
            }
            int length = length();
            if (i3 < length) {
                if (i < this.runCount - 1) {
                    i4 = this.runStarts[i + 1];
                    while (i4 <= i3) {
                        i++;
                        if (!valuesMatch(attribute2, getAttribute(attribute, i))) {
                            break;
                        }
                        if (i < this.runCount - 1) {
                            i4 = this.runStarts[i + 1];
                        }
                    }
                    if (i4 > i3) {
                        return null;
                    }
                }
                i4 = length;
            }
        }
        return attribute2;
    }

    private boolean attributeValuesMatch(Set<? extends AttributedCharacterIterator.Attribute> set, int i, int i2) {
        for (AttributedCharacterIterator.Attribute attribute : set) {
            if (!valuesMatch(getAttribute(attribute, i), getAttribute(attribute, i2))) {
                return false;
            }
        }
        return true;
    }

    private static final boolean valuesMatch(Object obj, Object obj2) {
        if (obj == null) {
            return obj2 == null;
        }
        return obj.equals(obj2);
    }

    private final void appendContents(StringBuffer stringBuffer, CharacterIterator characterIterator) {
        int endIndex = characterIterator.getEndIndex();
        for (int beginIndex = characterIterator.getBeginIndex(); beginIndex < endIndex; beginIndex++) {
            characterIterator.setIndex(beginIndex);
            stringBuffer.append(characterIterator.current());
        }
    }

    private void setAttributes(Map<AttributedCharacterIterator.Attribute, Object> map, int i) {
        int size;
        if (this.runCount == 0) {
            createRunAttributeDataVectors();
        }
        int iEnsureRunBreak = ensureRunBreak(i, false);
        if (map != null && (size = map.size()) > 0) {
            Vector<AttributedCharacterIterator.Attribute> vector = new Vector<>(size);
            Vector<Object> vector2 = new Vector<>(size);
            for (Map.Entry<AttributedCharacterIterator.Attribute, Object> entry : map.entrySet()) {
                vector.add(entry.getKey());
                vector2.add(entry.getValue());
            }
            this.runAttributes[iEnsureRunBreak] = vector;
            this.runAttributeValues[iEnsureRunBreak] = vector2;
        }
    }

    private static <K, V> boolean mapsDiffer(Map<K, V> map, Map<K, V> map2) {
        if (map == null) {
            return map2 != null && map2.size() > 0;
        }
        return !map.equals(map2);
    }

    private final class AttributedStringIterator implements AttributedCharacterIterator {
        private int beginIndex;
        private int currentIndex;
        private int currentRunIndex;
        private int currentRunLimit;
        private int currentRunStart;
        private int endIndex;
        private AttributedCharacterIterator.Attribute[] relevantAttributes;

        AttributedStringIterator(AttributedCharacterIterator.Attribute[] attributeArr, int i, int i2) {
            if (i < 0 || i > i2 || i2 > AttributedString.this.length()) {
                throw new IllegalArgumentException("Invalid substring range");
            }
            this.beginIndex = i;
            this.endIndex = i2;
            this.currentIndex = i;
            updateRunInfo();
            if (attributeArr != null) {
                this.relevantAttributes = (AttributedCharacterIterator.Attribute[]) attributeArr.clone();
            }
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof AttributedStringIterator)) {
                return false;
            }
            AttributedStringIterator attributedStringIterator = (AttributedStringIterator) obj;
            return AttributedString.this == attributedStringIterator.getString() && this.currentIndex == attributedStringIterator.currentIndex && this.beginIndex == attributedStringIterator.beginIndex && this.endIndex == attributedStringIterator.endIndex;
        }

        public int hashCode() {
            return ((AttributedString.this.text.hashCode() ^ this.currentIndex) ^ this.beginIndex) ^ this.endIndex;
        }

        @Override
        public Object clone() {
            try {
                return (AttributedStringIterator) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new InternalError(e);
            }
        }

        @Override
        public char first() {
            return internalSetIndex(this.beginIndex);
        }

        @Override
        public char last() {
            if (this.endIndex == this.beginIndex) {
                return internalSetIndex(this.endIndex);
            }
            return internalSetIndex(this.endIndex - 1);
        }

        @Override
        public char current() {
            if (this.currentIndex != this.endIndex) {
                return AttributedString.this.charAt(this.currentIndex);
            }
            return (char) 65535;
        }

        @Override
        public char next() {
            if (this.currentIndex < this.endIndex) {
                return internalSetIndex(this.currentIndex + 1);
            }
            return (char) 65535;
        }

        @Override
        public char previous() {
            if (this.currentIndex > this.beginIndex) {
                return internalSetIndex(this.currentIndex - 1);
            }
            return (char) 65535;
        }

        @Override
        public char setIndex(int i) {
            if (i < this.beginIndex || i > this.endIndex) {
                throw new IllegalArgumentException("Invalid index");
            }
            return internalSetIndex(i);
        }

        @Override
        public int getBeginIndex() {
            return this.beginIndex;
        }

        @Override
        public int getEndIndex() {
            return this.endIndex;
        }

        @Override
        public int getIndex() {
            return this.currentIndex;
        }

        @Override
        public int getRunStart() {
            return this.currentRunStart;
        }

        @Override
        public int getRunStart(AttributedCharacterIterator.Attribute attribute) {
            if (this.currentRunStart == this.beginIndex || this.currentRunIndex == -1) {
                return this.currentRunStart;
            }
            Object attribute2 = getAttribute(attribute);
            int i = this.currentRunStart;
            int i2 = this.currentRunIndex;
            while (i > this.beginIndex && AttributedString.valuesMatch(attribute2, AttributedString.this.getAttribute(attribute, i2 - 1))) {
                i2--;
                i = AttributedString.this.runStarts[i2];
            }
            if (i < this.beginIndex) {
                return this.beginIndex;
            }
            return i;
        }

        @Override
        public int getRunStart(Set<? extends AttributedCharacterIterator.Attribute> set) {
            if (this.currentRunStart == this.beginIndex || this.currentRunIndex == -1) {
                return this.currentRunStart;
            }
            int i = this.currentRunStart;
            int i2 = this.currentRunIndex;
            while (i > this.beginIndex && AttributedString.this.attributeValuesMatch(set, this.currentRunIndex, i2 - 1)) {
                i2--;
                i = AttributedString.this.runStarts[i2];
            }
            if (i < this.beginIndex) {
                return this.beginIndex;
            }
            return i;
        }

        @Override
        public int getRunLimit() {
            return this.currentRunLimit;
        }

        @Override
        public int getRunLimit(AttributedCharacterIterator.Attribute attribute) {
            if (this.currentRunLimit == this.endIndex || this.currentRunIndex == -1) {
                return this.currentRunLimit;
            }
            Object attribute2 = getAttribute(attribute);
            int i = this.currentRunLimit;
            int i2 = this.currentRunIndex;
            while (i < this.endIndex) {
                i2++;
                if (!AttributedString.valuesMatch(attribute2, AttributedString.this.getAttribute(attribute, i2))) {
                    break;
                }
                i = i2 < AttributedString.this.runCount + (-1) ? AttributedString.this.runStarts[i2 + 1] : this.endIndex;
            }
            if (i > this.endIndex) {
                return this.endIndex;
            }
            return i;
        }

        @Override
        public int getRunLimit(Set<? extends AttributedCharacterIterator.Attribute> set) {
            if (this.currentRunLimit == this.endIndex || this.currentRunIndex == -1) {
                return this.currentRunLimit;
            }
            int i = this.currentRunLimit;
            int i2 = this.currentRunIndex;
            while (i < this.endIndex) {
                i2++;
                if (!AttributedString.this.attributeValuesMatch(set, this.currentRunIndex, i2)) {
                    break;
                }
                i = i2 < AttributedString.this.runCount + (-1) ? AttributedString.this.runStarts[i2 + 1] : this.endIndex;
            }
            if (i > this.endIndex) {
                return this.endIndex;
            }
            return i;
        }

        @Override
        public Map<AttributedCharacterIterator.Attribute, Object> getAttributes() {
            if (AttributedString.this.runAttributes == null || this.currentRunIndex == -1 || AttributedString.this.runAttributes[this.currentRunIndex] == null) {
                return new Hashtable();
            }
            return AttributedString.this.new AttributeMap(this.currentRunIndex, this.beginIndex, this.endIndex);
        }

        @Override
        public Set<AttributedCharacterIterator.Attribute> getAllAttributeKeys() {
            HashSet hashSet;
            Vector<AttributedCharacterIterator.Attribute> vector;
            if (AttributedString.this.runAttributes == null) {
                return new HashSet();
            }
            synchronized (AttributedString.this) {
                hashSet = new HashSet();
                for (int i = 0; i < AttributedString.this.runCount; i++) {
                    if (AttributedString.this.runStarts[i] < this.endIndex && ((i == AttributedString.this.runCount - 1 || AttributedString.this.runStarts[i + 1] > this.beginIndex) && (vector = AttributedString.this.runAttributes[i]) != null)) {
                        int size = vector.size();
                        while (true) {
                            int i2 = size - 1;
                            if (size > 0) {
                                hashSet.add(vector.get(i2));
                                size = i2;
                            }
                        }
                    }
                }
            }
            return hashSet;
        }

        @Override
        public Object getAttribute(AttributedCharacterIterator.Attribute attribute) {
            int i = this.currentRunIndex;
            if (i >= 0) {
                return AttributedString.this.getAttributeCheckRange(attribute, i, this.beginIndex, this.endIndex);
            }
            return null;
        }

        private AttributedString getString() {
            return AttributedString.this;
        }

        private char internalSetIndex(int i) {
            this.currentIndex = i;
            if (i < this.currentRunStart || i >= this.currentRunLimit) {
                updateRunInfo();
            }
            if (this.currentIndex != this.endIndex) {
                return AttributedString.this.charAt(i);
            }
            return (char) 65535;
        }

        private void updateRunInfo() {
            int i = -1;
            if (this.currentIndex == this.endIndex) {
                int i2 = this.endIndex;
                this.currentRunLimit = i2;
                this.currentRunStart = i2;
                this.currentRunIndex = -1;
                return;
            }
            synchronized (AttributedString.this) {
                while (i < AttributedString.this.runCount - 1) {
                    int i3 = i + 1;
                    if (AttributedString.this.runStarts[i3] > this.currentIndex) {
                        break;
                    } else {
                        i = i3;
                    }
                }
                this.currentRunIndex = i;
                if (i >= 0) {
                    this.currentRunStart = AttributedString.this.runStarts[i];
                    if (this.currentRunStart < this.beginIndex) {
                        this.currentRunStart = this.beginIndex;
                    }
                } else {
                    this.currentRunStart = this.beginIndex;
                }
                if (i < AttributedString.this.runCount - 1) {
                    this.currentRunLimit = AttributedString.this.runStarts[i + 1];
                    if (this.currentRunLimit > this.endIndex) {
                        this.currentRunLimit = this.endIndex;
                    }
                } else {
                    this.currentRunLimit = this.endIndex;
                }
            }
        }
    }

    private final class AttributeMap extends AbstractMap<AttributedCharacterIterator.Attribute, Object> {
        int beginIndex;
        int endIndex;
        int runIndex;

        AttributeMap(int i, int i2, int i3) {
            this.runIndex = i;
            this.beginIndex = i2;
            this.endIndex = i3;
        }

        @Override
        public Set<Map.Entry<AttributedCharacterIterator.Attribute, Object>> entrySet() {
            HashSet hashSet = new HashSet();
            synchronized (AttributedString.this) {
                int size = AttributedString.this.runAttributes[this.runIndex].size();
                for (int i = 0; i < size; i++) {
                    AttributedCharacterIterator.Attribute attribute = AttributedString.this.runAttributes[this.runIndex].get(i);
                    Object attributeCheckRange = AttributedString.this.runAttributeValues[this.runIndex].get(i);
                    if (!(attributeCheckRange instanceof Annotation) || (attributeCheckRange = AttributedString.this.getAttributeCheckRange(attribute, this.runIndex, this.beginIndex, this.endIndex)) != null) {
                        hashSet.add(new AttributeEntry(attribute, attributeCheckRange));
                    }
                }
            }
            return hashSet;
        }

        @Override
        public Object get(Object obj) {
            return AttributedString.this.getAttributeCheckRange((AttributedCharacterIterator.Attribute) obj, this.runIndex, this.beginIndex, this.endIndex);
        }
    }
}
