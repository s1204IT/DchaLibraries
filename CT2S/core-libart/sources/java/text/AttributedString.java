package java.text;

import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

public class AttributedString {
    Map<AttributedCharacterIterator.Attribute, List<Range>> attributeMap;
    String text;

    static class Range {
        int end;
        int start;
        Object value;

        Range(int s, int e, Object v) {
            this.start = s;
            this.end = e;
            this.value = v;
        }
    }

    static class AttributedIterator implements AttributedCharacterIterator {
        private AttributedString attrString;
        private HashSet<AttributedCharacterIterator.Attribute> attributesAllowed;
        private int begin;
        private int end;
        private int offset;

        AttributedIterator(AttributedString attrString) {
            this.attrString = attrString;
            this.begin = 0;
            this.end = attrString.text.length();
            this.offset = 0;
        }

        AttributedIterator(AttributedString attrString, AttributedCharacterIterator.Attribute[] attributes, int begin, int end) {
            if (begin < 0 || end > attrString.text.length() || begin > end) {
                throw new IllegalArgumentException();
            }
            this.begin = begin;
            this.end = end;
            this.offset = begin;
            this.attrString = attrString;
            if (attributes != null) {
                HashSet<AttributedCharacterIterator.Attribute> set = new HashSet<>(((attributes.length * 4) / 3) + 1);
                int i = attributes.length;
                while (true) {
                    i--;
                    if (i >= 0) {
                        set.add(attributes[i]);
                    } else {
                        this.attributesAllowed = set;
                        return;
                    }
                }
            }
        }

        @Override
        public Object clone() {
            try {
                AttributedIterator clone = (AttributedIterator) super.clone();
                if (this.attributesAllowed != null) {
                    clone.attributesAllowed = (HashSet) this.attributesAllowed.clone();
                }
                return clone;
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(e);
            }
        }

        @Override
        public char current() {
            if (this.offset == this.end) {
                return (char) 65535;
            }
            return this.attrString.text.charAt(this.offset);
        }

        @Override
        public char first() {
            if (this.begin == this.end) {
                return (char) 65535;
            }
            this.offset = this.begin;
            return this.attrString.text.charAt(this.offset);
        }

        @Override
        public int getBeginIndex() {
            return this.begin;
        }

        @Override
        public int getEndIndex() {
            return this.end;
        }

        @Override
        public int getIndex() {
            return this.offset;
        }

        private boolean inRange(Range range) {
            if (range.value instanceof Annotation) {
                return range.start >= this.begin && range.start < this.end && range.end > this.begin && range.end <= this.end;
            }
            return true;
        }

        private boolean inRange(List<Range> ranges) {
            for (Range range : ranges) {
                if (range.start >= this.begin && range.start < this.end) {
                    return !(range.value instanceof Annotation) || (range.end > this.begin && range.end <= this.end);
                }
                if (range.end > this.begin && range.end <= this.end) {
                    return !(range.value instanceof Annotation) || (range.start >= this.begin && range.start < this.end);
                }
            }
            return false;
        }

        @Override
        public Set<AttributedCharacterIterator.Attribute> getAllAttributeKeys() {
            if (this.begin == 0 && this.end == this.attrString.text.length() && this.attributesAllowed == null) {
                return this.attrString.attributeMap.keySet();
            }
            Set<AttributedCharacterIterator.Attribute> result = new HashSet<>(((this.attrString.attributeMap.size() * 4) / 3) + 1);
            for (Map.Entry<AttributedCharacterIterator.Attribute, List<Range>> entry : this.attrString.attributeMap.entrySet()) {
                if (this.attributesAllowed == null || this.attributesAllowed.contains(entry.getKey())) {
                    List<Range> ranges = entry.getValue();
                    if (inRange(ranges)) {
                        result.add(entry.getKey());
                    }
                }
            }
            return result;
        }

        private Object currentValue(List<Range> ranges) {
            for (Range range : ranges) {
                if (this.offset >= range.start && this.offset < range.end) {
                    if (inRange(range)) {
                        return range.value;
                    }
                    return null;
                }
            }
            return null;
        }

        @Override
        public Object getAttribute(AttributedCharacterIterator.Attribute attribute) {
            ArrayList<Range> ranges;
            if ((this.attributesAllowed == null || this.attributesAllowed.contains(attribute)) && (ranges = (ArrayList) this.attrString.attributeMap.get(attribute)) != null) {
                return currentValue(ranges);
            }
            return null;
        }

        @Override
        public Map<AttributedCharacterIterator.Attribute, Object> getAttributes() {
            Map<AttributedCharacterIterator.Attribute, Object> result = new HashMap<>(((this.attrString.attributeMap.size() * 4) / 3) + 1);
            for (Map.Entry<AttributedCharacterIterator.Attribute, List<Range>> entry : this.attrString.attributeMap.entrySet()) {
                if (this.attributesAllowed == null || this.attributesAllowed.contains(entry.getKey())) {
                    Object value = currentValue(entry.getValue());
                    if (value != null) {
                        result.put(entry.getKey(), value);
                    }
                }
            }
            return result;
        }

        @Override
        public int getRunLimit() {
            return getRunLimit(getAllAttributeKeys());
        }

        private int runLimit(List<Range> ranges) {
            int result = this.end;
            ListIterator<Range> it = ranges.listIterator(ranges.size());
            while (it.hasPrevious()) {
                Range range = it.previous();
                if (range.end > this.begin) {
                    if (this.offset >= range.start && this.offset < range.end) {
                        if (inRange(range)) {
                            int result2 = range.end;
                            return result2;
                        }
                        return result;
                    }
                    if (this.offset < range.end) {
                        result = range.start;
                    } else {
                        return result;
                    }
                } else {
                    return result;
                }
            }
            return result;
        }

        @Override
        public int getRunLimit(AttributedCharacterIterator.Attribute attribute) {
            if (this.attributesAllowed != null && !this.attributesAllowed.contains(attribute)) {
                return this.end;
            }
            ArrayList<Range> ranges = (ArrayList) this.attrString.attributeMap.get(attribute);
            if (ranges == null) {
                return this.end;
            }
            return runLimit(ranges);
        }

        @Override
        public int getRunLimit(Set<? extends AttributedCharacterIterator.Attribute> attributes) {
            int limit = this.end;
            for (AttributedCharacterIterator.Attribute attribute : attributes) {
                int newLimit = getRunLimit(attribute);
                if (newLimit < limit) {
                    limit = newLimit;
                }
            }
            return limit;
        }

        @Override
        public int getRunStart() {
            return getRunStart(getAllAttributeKeys());
        }

        private int runStart(List<Range> ranges) {
            int result = this.begin;
            for (Range range : ranges) {
                if (range.start < this.end) {
                    if (this.offset >= range.start && this.offset < range.end) {
                        if (inRange(range)) {
                            int result2 = range.start;
                            return result2;
                        }
                        return result;
                    }
                    if (this.offset >= range.start) {
                        result = range.end;
                    } else {
                        return result;
                    }
                } else {
                    return result;
                }
            }
            return result;
        }

        @Override
        public int getRunStart(AttributedCharacterIterator.Attribute attribute) {
            if (this.attributesAllowed != null && !this.attributesAllowed.contains(attribute)) {
                return this.begin;
            }
            ArrayList<Range> ranges = (ArrayList) this.attrString.attributeMap.get(attribute);
            if (ranges == null) {
                return this.begin;
            }
            return runStart(ranges);
        }

        @Override
        public int getRunStart(Set<? extends AttributedCharacterIterator.Attribute> attributes) {
            int start = this.begin;
            for (AttributedCharacterIterator.Attribute attribute : attributes) {
                int newStart = getRunStart(attribute);
                if (newStart > start) {
                    start = newStart;
                }
            }
            return start;
        }

        @Override
        public char last() {
            if (this.begin == this.end) {
                return (char) 65535;
            }
            this.offset = this.end - 1;
            return this.attrString.text.charAt(this.offset);
        }

        @Override
        public char next() {
            if (this.offset >= this.end - 1) {
                this.offset = this.end;
                return (char) 65535;
            }
            String str = this.attrString.text;
            int i = this.offset + 1;
            this.offset = i;
            return str.charAt(i);
        }

        @Override
        public char previous() {
            if (this.offset == this.begin) {
                return (char) 65535;
            }
            String str = this.attrString.text;
            int i = this.offset - 1;
            this.offset = i;
            return str.charAt(i);
        }

        @Override
        public char setIndex(int location) {
            if (location < this.begin || location > this.end) {
                throw new IllegalArgumentException();
            }
            this.offset = location;
            if (this.offset == this.end) {
                return (char) 65535;
            }
            return this.attrString.text.charAt(this.offset);
        }
    }

    public AttributedString(AttributedCharacterIterator iterator) {
        if (iterator.getBeginIndex() > iterator.getEndIndex()) {
            throw new IllegalArgumentException("Invalid substring range");
        }
        StringBuilder buffer = new StringBuilder();
        for (int i = iterator.getBeginIndex(); i < iterator.getEndIndex(); i++) {
            buffer.append(iterator.current());
            iterator.next();
        }
        this.text = buffer.toString();
        Set<AttributedCharacterIterator.Attribute> attributes = iterator.getAllAttributeKeys();
        if (attributes != null) {
            this.attributeMap = new HashMap(((attributes.size() * 4) / 3) + 1);
            for (AttributedCharacterIterator.Attribute attribute : attributes) {
                iterator.setIndex(0);
                while (iterator.current() != 65535) {
                    int start = iterator.getRunStart(attribute);
                    int limit = iterator.getRunLimit(attribute);
                    Object value = iterator.getAttribute(attribute);
                    if (value != null) {
                        addAttribute(attribute, value, start, limit);
                    }
                    iterator.setIndex(limit);
                }
            }
        }
    }

    private AttributedString(AttributedCharacterIterator iterator, int start, int end, Set<AttributedCharacterIterator.Attribute> attributes) {
        if (start < iterator.getBeginIndex() || end > iterator.getEndIndex() || start > end) {
            throw new IllegalArgumentException();
        }
        if (attributes != null) {
            StringBuilder buffer = new StringBuilder();
            iterator.setIndex(start);
            while (iterator.getIndex() < end) {
                buffer.append(iterator.current());
                iterator.next();
            }
            this.text = buffer.toString();
            this.attributeMap = new HashMap(((attributes.size() * 4) / 3) + 1);
            for (AttributedCharacterIterator.Attribute attribute : attributes) {
                iterator.setIndex(start);
                while (iterator.getIndex() < end) {
                    Object value = iterator.getAttribute(attribute);
                    int runStart = iterator.getRunStart(attribute);
                    int limit = iterator.getRunLimit(attribute);
                    if (((value instanceof Annotation) && runStart >= start && limit <= end) || (value != null && !(value instanceof Annotation))) {
                        addAttribute(attribute, value, (runStart < start ? start : runStart) - start, (limit > end ? end : limit) - start);
                    }
                    iterator.setIndex(limit);
                }
            }
        }
    }

    public AttributedString(AttributedCharacterIterator iterator, int start, int end) {
        this(iterator, start, end, iterator.getAllAttributeKeys());
    }

    public AttributedString(AttributedCharacterIterator iterator, int start, int end, AttributedCharacterIterator.Attribute[] attributes) {
        this(iterator, start, end, attributes == null ? new HashSet() : new HashSet(Arrays.asList(attributes)));
    }

    public AttributedString(String value) {
        if (value == null) {
            throw new NullPointerException("value == null");
        }
        this.text = value;
        this.attributeMap = new HashMap(11);
    }

    public AttributedString(String value, Map<? extends AttributedCharacterIterator.Attribute, ?> attributes) {
        if (value == null) {
            throw new NullPointerException("value == null");
        }
        if (value.length() == 0 && !attributes.isEmpty()) {
            throw new IllegalArgumentException("Cannot add attributes to empty string");
        }
        this.text = value;
        this.attributeMap = new HashMap(((attributes.size() * 4) / 3) + 1);
        for (Map.Entry<? extends AttributedCharacterIterator.Attribute, ?> entry : attributes.entrySet()) {
            ArrayList<Range> ranges = new ArrayList<>(1);
            ranges.add(new Range(0, this.text.length(), entry.getValue()));
            this.attributeMap.put(entry.getKey(), ranges);
        }
    }

    public void addAttribute(AttributedCharacterIterator.Attribute attribute, Object value) {
        if (attribute == null) {
            throw new NullPointerException("attribute == null");
        }
        if (this.text.isEmpty()) {
            throw new IllegalArgumentException("text is empty");
        }
        List<Range> ranges = this.attributeMap.get(attribute);
        if (ranges == null) {
            ranges = new ArrayList<>(1);
            this.attributeMap.put(attribute, ranges);
        } else {
            ranges.clear();
        }
        ranges.add(new Range(0, this.text.length(), value));
    }

    public void addAttribute(AttributedCharacterIterator.Attribute attribute, Object value, int start, int end) {
        Range range;
        if (attribute == null) {
            throw new NullPointerException("attribute == null");
        }
        if (start < 0 || end > this.text.length() || start >= end) {
            throw new IllegalArgumentException();
        }
        if (value != null) {
            List<Range> ranges = this.attributeMap.get(attribute);
            if (ranges == null) {
                List<Range> ranges2 = new ArrayList<>(1);
                ranges2.add(new Range(start, end, value));
                this.attributeMap.put(attribute, ranges2);
                return;
            }
            ListIterator<Range> it = ranges.listIterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                range = it.next();
                if (end <= range.start) {
                    it.previous();
                    break;
                } else if (start < range.end || (start == range.end && value.equals(range.value))) {
                    break;
                }
            }
            it.remove();
            Range r1 = new Range(range.start, start, range.value);
            Range r3 = new Range(end, range.end, range.value);
            while (end > range.end && it.hasNext()) {
                range = it.next();
                if (end <= range.end) {
                    if (end > range.start || (end == range.start && value.equals(range.value))) {
                        it.remove();
                        r3 = new Range(end, range.end, range.value);
                        break;
                    }
                } else {
                    it.remove();
                }
            }
            if (value.equals(r1.value)) {
                if (value.equals(r3.value)) {
                    if (r1.start < start) {
                        start = r1.start;
                    }
                    if (r3.end > end) {
                        end = r3.end;
                    }
                    it.add(new Range(start, end, r1.value));
                    return;
                }
                if (r1.start < start) {
                    start = r1.start;
                }
                it.add(new Range(start, end, r1.value));
                if (r3.start < r3.end) {
                    it.add(r3);
                    return;
                }
                return;
            }
            if (value.equals(r3.value)) {
                if (r1.start < r1.end) {
                    it.add(r1);
                }
                if (r3.end > end) {
                    end = r3.end;
                }
                it.add(new Range(start, end, r3.value));
                return;
            }
            if (r1.start < r1.end) {
                it.add(r1);
            }
            it.add(new Range(start, end, value));
            if (r3.start < r3.end) {
                it.add(r3);
            }
        }
    }

    public void addAttributes(Map<? extends AttributedCharacterIterator.Attribute, ?> attributes, int start, int end) {
        for (Map.Entry<? extends AttributedCharacterIterator.Attribute, ?> entry : attributes.entrySet()) {
            addAttribute(entry.getKey(), entry.getValue(), start, end);
        }
    }

    public AttributedCharacterIterator getIterator() {
        return new AttributedIterator(this);
    }

    public AttributedCharacterIterator getIterator(AttributedCharacterIterator.Attribute[] attributes) {
        return new AttributedIterator(this, attributes, 0, this.text.length());
    }

    public AttributedCharacterIterator getIterator(AttributedCharacterIterator.Attribute[] attributes, int start, int end) {
        return new AttributedIterator(this, attributes, start, end);
    }
}
