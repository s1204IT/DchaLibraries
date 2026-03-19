package android.icu.util;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class StringTrieBuilder {

    private static final int[] f112androidicuutilStringTrieBuilder$StateSwitchesValues = null;

    static final boolean f113assertionsDisabled;
    private Node root;
    private State state = State.ADDING;

    @Deprecated
    protected StringBuilder strings = new StringBuilder();
    private HashMap<Node, Node> nodes = new HashMap<>();
    private ValueNode lookupFinalValueNode = new ValueNode();

    private static int[] m289getandroidicuutilStringTrieBuilder$StateSwitchesValues() {
        if (f112androidicuutilStringTrieBuilder$StateSwitchesValues != null) {
            return f112androidicuutilStringTrieBuilder$StateSwitchesValues;
        }
        int[] iArr = new int[State.valuesCustom().length];
        try {
            iArr[State.ADDING.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[State.BUILDING_FAST.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[State.BUILDING_SMALL.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[State.BUILT.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        f112androidicuutilStringTrieBuilder$StateSwitchesValues = iArr;
        return iArr;
    }

    @Deprecated
    protected abstract int getMaxBranchLinearSubNodeLength();

    @Deprecated
    protected abstract int getMaxLinearMatchLength();

    @Deprecated
    protected abstract int getMinLinearMatch();

    @Deprecated
    protected abstract boolean matchNodesCanHaveValues();

    @Deprecated
    protected abstract int write(int i);

    @Deprecated
    protected abstract int write(int i, int i2);

    @Deprecated
    protected abstract int writeDeltaTo(int i);

    @Deprecated
    protected abstract int writeValueAndFinal(int i, boolean z);

    @Deprecated
    protected abstract int writeValueAndType(boolean z, int i, int i2);

    static {
        f113assertionsDisabled = !StringTrieBuilder.class.desiredAssertionStatus();
    }

    public enum Option {
        FAST,
        SMALL;

        public static Option[] valuesCustom() {
            return values();
        }
    }

    @Deprecated
    protected StringTrieBuilder() {
    }

    @Deprecated
    protected void addImpl(CharSequence s, int value) {
        if (this.state != State.ADDING) {
            throw new IllegalStateException("Cannot add (string, value) pairs after build().");
        }
        if (s.length() > 65535) {
            throw new IndexOutOfBoundsException("The maximum string length is 0xffff.");
        }
        if (this.root == null) {
            this.root = createSuffixNode(s, 0, value);
        } else {
            this.root = this.root.add(this, s, 0, value);
        }
    }

    @Deprecated
    protected final void buildImpl(Option buildOption) {
        switch (m289getandroidicuutilStringTrieBuilder$StateSwitchesValues()[this.state.ordinal()]) {
            case 1:
                if (this.root == null) {
                    throw new IndexOutOfBoundsException("No (string, value) pairs were added.");
                }
                if (buildOption == Option.FAST) {
                    this.state = State.BUILDING_FAST;
                } else {
                    this.state = State.BUILDING_SMALL;
                }
                break;
                break;
            case 2:
            case 3:
                throw new IllegalStateException("Builder failed and must be clear()ed.");
            case 4:
                return;
        }
        this.root = this.root.register(this);
        this.root.markRightEdgesFirst(-1);
        this.root.write(this);
        this.state = State.BUILT;
    }

    @Deprecated
    protected void clearImpl() {
        this.strings.setLength(0);
        this.nodes.clear();
        this.root = null;
        this.state = State.ADDING;
    }

    private final Node registerNode(Node newNode) {
        if (this.state == State.BUILDING_FAST) {
            return newNode;
        }
        Node oldNode = this.nodes.get(newNode);
        if (oldNode != null) {
            return oldNode;
        }
        Node oldNode2 = this.nodes.put(newNode, newNode);
        if (!f113assertionsDisabled) {
            if (!(oldNode2 == null)) {
                throw new AssertionError();
            }
        }
        return newNode;
    }

    private final ValueNode registerFinalValue(int value) {
        this.lookupFinalValueNode.setFinalValue(value);
        Node oldNode = this.nodes.get(this.lookupFinalValueNode);
        if (oldNode != null) {
            return (ValueNode) oldNode;
        }
        ValueNode newNode = new ValueNode(value);
        Node oldNode2 = this.nodes.put(newNode, newNode);
        if (!f113assertionsDisabled) {
            if (!(oldNode2 == null)) {
                throw new AssertionError();
            }
        }
        return newNode;
    }

    private static abstract class Node {
        protected int offset = 0;

        public abstract int hashCode();

        public abstract void write(StringTrieBuilder stringTrieBuilder);

        public boolean equals(Object other) {
            return this == other || getClass() == other.getClass();
        }

        public Node add(StringTrieBuilder builder, CharSequence s, int start, int sValue) {
            return this;
        }

        public Node register(StringTrieBuilder builder) {
            return this;
        }

        public int markRightEdgesFirst(int edgeNumber) {
            if (this.offset == 0) {
                this.offset = edgeNumber;
            }
            return edgeNumber;
        }

        public final void writeUnlessInsideRightEdge(int firstRight, int lastRight, StringTrieBuilder builder) {
            if (this.offset < 0) {
                if (this.offset >= lastRight && firstRight >= this.offset) {
                    return;
                }
                write(builder);
            }
        }

        public final int getOffset() {
            return this.offset;
        }
    }

    private static class ValueNode extends Node {

        static final boolean f116assertionsDisabled;
        protected boolean hasValue;
        protected int value;

        static {
            f116assertionsDisabled = !ValueNode.class.desiredAssertionStatus();
        }

        public ValueNode() {
        }

        public ValueNode(int v) {
            this.hasValue = true;
            this.value = v;
        }

        public final void setValue(int v) {
            if (!f116assertionsDisabled) {
                if (!(!this.hasValue)) {
                    throw new AssertionError();
                }
            }
            this.hasValue = true;
            this.value = v;
        }

        private void setFinalValue(int v) {
            this.hasValue = true;
            this.value = v;
        }

        @Override
        public int hashCode() {
            if (!this.hasValue) {
                return 1118481;
            }
            int hash = 41383797 + this.value;
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!super.equals(other)) {
                return false;
            }
            ValueNode o = (ValueNode) other;
            return this.hasValue == o.hasValue && (!this.hasValue || this.value == o.value);
        }

        @Override
        public Node add(StringTrieBuilder builder, CharSequence s, int start, int sValue) {
            if (start == s.length()) {
                throw new IllegalArgumentException("Duplicate string.");
            }
            ValueNode node = builder.createSuffixNode(s, start, sValue);
            node.setValue(this.value);
            return node;
        }

        @Override
        public void write(StringTrieBuilder builder) {
            this.offset = builder.writeValueAndFinal(this.value, true);
        }
    }

    private static final class IntermediateValueNode extends ValueNode {
        private Node next;

        public IntermediateValueNode(int v, Node nextNode) {
            this.next = nextNode;
            setValue(v);
        }

        @Override
        public int hashCode() {
            return ((this.value + 82767594) * 37) + this.next.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!super.equals(other)) {
                return false;
            }
            IntermediateValueNode o = (IntermediateValueNode) other;
            return this.next == o.next;
        }

        @Override
        public int markRightEdgesFirst(int edgeNumber) {
            if (this.offset == 0) {
                int edgeNumber2 = this.next.markRightEdgesFirst(edgeNumber);
                this.offset = edgeNumber2;
                return edgeNumber2;
            }
            return edgeNumber;
        }

        @Override
        public void write(StringTrieBuilder builder) {
            this.next.write(builder);
            this.offset = builder.writeValueAndFinal(this.value, false);
        }
    }

    private static final class LinearMatchNode extends ValueNode {
        private int hash;
        private int length;
        private Node next;
        private int stringOffset;
        private CharSequence strings;

        public LinearMatchNode(CharSequence builderStrings, int sOffset, int len, Node nextNode) {
            this.strings = builderStrings;
            this.stringOffset = sOffset;
            this.length = len;
            this.next = nextNode;
        }

        @Override
        public int hashCode() {
            return this.hash;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!super.equals(other)) {
                return false;
            }
            LinearMatchNode o = (LinearMatchNode) other;
            if (this.length != o.length || this.next != o.next) {
                return false;
            }
            int i = this.stringOffset;
            int j = o.stringOffset;
            int limit = this.stringOffset + this.length;
            while (i < limit) {
                if (this.strings.charAt(i) != this.strings.charAt(j)) {
                    return false;
                }
                i++;
                j++;
            }
            return true;
        }

        @Override
        public Node add(StringTrieBuilder builder, CharSequence s, int start, int sValue) {
            Node thisSuffixNode;
            Node result;
            if (start == s.length()) {
                if (this.hasValue) {
                    throw new IllegalArgumentException("Duplicate string.");
                }
                setValue(sValue);
                return this;
            }
            int limit = this.stringOffset + this.length;
            int i = this.stringOffset;
            while (i < limit) {
                if (start == s.length()) {
                    int prefixLength = i - this.stringOffset;
                    LinearMatchNode suffixNode = new LinearMatchNode(this.strings, i, this.length - prefixLength, this.next);
                    suffixNode.setValue(sValue);
                    this.length = prefixLength;
                    this.next = suffixNode;
                    return this;
                }
                char thisChar = this.strings.charAt(i);
                char newChar = s.charAt(start);
                if (thisChar == newChar) {
                    i++;
                    start++;
                } else {
                    DynamicBranchNode branchNode = new DynamicBranchNode();
                    if (i == this.stringOffset) {
                        if (this.hasValue) {
                            branchNode.setValue(this.value);
                            this.value = 0;
                            this.hasValue = false;
                        }
                        this.stringOffset++;
                        this.length--;
                        thisSuffixNode = this.length > 0 ? this : this.next;
                        result = branchNode;
                    } else if (i == limit - 1) {
                        this.length--;
                        thisSuffixNode = this.next;
                        this.next = branchNode;
                        result = this;
                    } else {
                        int prefixLength2 = i - this.stringOffset;
                        thisSuffixNode = new LinearMatchNode(this.strings, i + 1, this.length - (prefixLength2 + 1), this.next);
                        this.length = prefixLength2;
                        this.next = branchNode;
                        result = this;
                    }
                    ValueNode newSuffixNode = builder.createSuffixNode(s, start + 1, sValue);
                    branchNode.add(thisChar, thisSuffixNode);
                    branchNode.add(newChar, newSuffixNode);
                    return result;
                }
            }
            this.next = this.next.add(builder, s, start, sValue);
            return this;
        }

        @Override
        public Node register(StringTrieBuilder builder) {
            Node result;
            this.next = this.next.register(builder);
            int maxLinearMatchLength = builder.getMaxLinearMatchLength();
            while (this.length > maxLinearMatchLength) {
                int nextOffset = (this.stringOffset + this.length) - maxLinearMatchLength;
                this.length -= maxLinearMatchLength;
                LinearMatchNode suffixNode = new LinearMatchNode(this.strings, nextOffset, maxLinearMatchLength, this.next);
                suffixNode.setHashCode();
                this.next = builder.registerNode(suffixNode);
            }
            if (this.hasValue && !builder.matchNodesCanHaveValues()) {
                int intermediateValue = this.value;
                this.value = 0;
                this.hasValue = false;
                setHashCode();
                result = new IntermediateValueNode(intermediateValue, builder.registerNode(this));
            } else {
                setHashCode();
                result = this;
            }
            return builder.registerNode(result);
        }

        @Override
        public int markRightEdgesFirst(int edgeNumber) {
            if (this.offset == 0) {
                int edgeNumber2 = this.next.markRightEdgesFirst(edgeNumber);
                this.offset = edgeNumber2;
                return edgeNumber2;
            }
            return edgeNumber;
        }

        @Override
        public void write(StringTrieBuilder builder) {
            this.next.write(builder);
            builder.write(this.stringOffset, this.length);
            this.offset = builder.writeValueAndType(this.hasValue, this.value, (builder.getMinLinearMatch() + this.length) - 1);
        }

        private void setHashCode() {
            this.hash = ((this.length + 124151391) * 37) + this.next.hashCode();
            if (this.hasValue) {
                this.hash = (this.hash * 37) + this.value;
            }
            int limit = this.stringOffset + this.length;
            for (int i = this.stringOffset; i < limit; i++) {
                this.hash = (this.hash * 37) + this.strings.charAt(i);
            }
        }
    }

    private static final class DynamicBranchNode extends ValueNode {
        private StringBuilder chars = new StringBuilder();
        private ArrayList<Node> equal = new ArrayList<>();

        public void add(char c, Node node) {
            int i = find(c);
            this.chars.insert(i, c);
            this.equal.add(i, node);
        }

        @Override
        public Node add(StringTrieBuilder builder, CharSequence s, int start, int sValue) {
            if (start == s.length()) {
                if (this.hasValue) {
                    throw new IllegalArgumentException("Duplicate string.");
                }
                setValue(sValue);
                return this;
            }
            int start2 = start + 1;
            char c = s.charAt(start);
            int i = find(c);
            if (i < this.chars.length() && c == this.chars.charAt(i)) {
                this.equal.set(i, this.equal.get(i).add(builder, s, start2, sValue));
            } else {
                this.chars.insert(i, c);
                this.equal.add(i, builder.createSuffixNode(s, start2, sValue));
            }
            return this;
        }

        @Override
        public Node register(StringTrieBuilder builder) {
            Node subNode = register(builder, 0, this.chars.length());
            BranchHeadNode head = new BranchHeadNode(this.chars.length(), subNode);
            Node result = head;
            if (this.hasValue) {
                if (builder.matchNodesCanHaveValues()) {
                    head.setValue(this.value);
                } else {
                    result = new IntermediateValueNode(this.value, builder.registerNode(head));
                }
            }
            return builder.registerNode(result);
        }

        private Node register(StringTrieBuilder builder, int start, int limit) {
            int length = limit - start;
            if (length > builder.getMaxBranchLinearSubNodeLength()) {
                int middle = start + (length / 2);
                return builder.registerNode(new SplitBranchNode(this.chars.charAt(middle), register(builder, start, middle), register(builder, middle, limit)));
            }
            ListBranchNode listNode = new ListBranchNode(length);
            do {
                char c = this.chars.charAt(start);
                Node node = this.equal.get(start);
                if (node.getClass() == ValueNode.class) {
                    listNode.add(c, ((ValueNode) node).value);
                } else {
                    listNode.add(c, node.register(builder));
                }
                start++;
            } while (start < limit);
            return builder.registerNode(listNode);
        }

        private int find(char c) {
            int start = 0;
            int limit = this.chars.length();
            while (start < limit) {
                int i = (start + limit) / 2;
                char middleChar = this.chars.charAt(i);
                if (c < middleChar) {
                    limit = i;
                } else {
                    if (c == middleChar) {
                        return i;
                    }
                    start = i + 1;
                }
            }
            return start;
        }
    }

    private static abstract class BranchNode extends Node {
        protected int firstEdgeNumber;
        protected int hash;

        @Override
        public int hashCode() {
            return this.hash;
        }
    }

    private static final class ListBranchNode extends BranchNode {

        static final boolean f114assertionsDisabled;
        private Node[] equal;
        private int length;
        private char[] units;
        private int[] values;

        static {
            f114assertionsDisabled = !ListBranchNode.class.desiredAssertionStatus();
        }

        public ListBranchNode(int capacity) {
            this.hash = 165535188 + capacity;
            this.equal = new Node[capacity];
            this.values = new int[capacity];
            this.units = new char[capacity];
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!super.equals(other)) {
                return false;
            }
            ListBranchNode o = (ListBranchNode) other;
            for (int i = 0; i < this.length; i++) {
                if (this.units[i] != o.units[i] || this.values[i] != o.values[i] || this.equal[i] != o.equal[i]) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public int markRightEdgesFirst(int edgeNumber) {
            if (this.offset == 0) {
                this.firstEdgeNumber = edgeNumber;
                int step = 0;
                int i = this.length;
                do {
                    i--;
                    Node edge = this.equal[i];
                    if (edge != null) {
                        edgeNumber = edge.markRightEdgesFirst(edgeNumber - step);
                    }
                    step = 1;
                } while (i > 0);
                this.offset = edgeNumber;
            }
            return edgeNumber;
        }

        @Override
        public void write(StringTrieBuilder builder) {
            int value;
            boolean isFinal;
            int unitNumber = this.length - 1;
            Node rightEdge = this.equal[unitNumber];
            int rightEdgeNumber = rightEdge == null ? this.firstEdgeNumber : rightEdge.getOffset();
            do {
                unitNumber--;
                if (this.equal[unitNumber] != null) {
                    this.equal[unitNumber].writeUnlessInsideRightEdge(this.firstEdgeNumber, rightEdgeNumber, builder);
                }
            } while (unitNumber > 0);
            int unitNumber2 = this.length - 1;
            if (rightEdge == null) {
                builder.writeValueAndFinal(this.values[unitNumber2], true);
            } else {
                rightEdge.write(builder);
            }
            this.offset = builder.write(this.units[unitNumber2]);
            while (true) {
                unitNumber2--;
                if (unitNumber2 < 0) {
                    return;
                }
                if (this.equal[unitNumber2] == null) {
                    value = this.values[unitNumber2];
                    isFinal = true;
                } else {
                    if (!f114assertionsDisabled) {
                        if (!(this.equal[unitNumber2].getOffset() > 0)) {
                            throw new AssertionError();
                        }
                    }
                    value = this.offset - this.equal[unitNumber2].getOffset();
                    isFinal = false;
                }
                builder.writeValueAndFinal(value, isFinal);
                this.offset = builder.write(this.units[unitNumber2]);
            }
        }

        public void add(int c, int value) {
            this.units[this.length] = (char) c;
            this.equal[this.length] = null;
            this.values[this.length] = value;
            this.length++;
            this.hash = (((this.hash * 37) + c) * 37) + value;
        }

        public void add(int c, Node node) {
            this.units[this.length] = (char) c;
            this.equal[this.length] = node;
            this.values[this.length] = 0;
            this.length++;
            this.hash = (((this.hash * 37) + c) * 37) + node.hashCode();
        }
    }

    private static final class SplitBranchNode extends BranchNode {

        static final boolean f115assertionsDisabled;
        private Node greaterOrEqual;
        private Node lessThan;
        private char unit;

        static {
            f115assertionsDisabled = !SplitBranchNode.class.desiredAssertionStatus();
        }

        public SplitBranchNode(char middleUnit, Node lessThanNode, Node greaterOrEqualNode) {
            this.hash = ((((206918985 + middleUnit) * 37) + lessThanNode.hashCode()) * 37) + greaterOrEqualNode.hashCode();
            this.unit = middleUnit;
            this.lessThan = lessThanNode;
            this.greaterOrEqual = greaterOrEqualNode;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!super.equals(other)) {
                return false;
            }
            SplitBranchNode o = (SplitBranchNode) other;
            return this.unit == o.unit && this.lessThan == o.lessThan && this.greaterOrEqual == o.greaterOrEqual;
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public int markRightEdgesFirst(int edgeNumber) {
            if (this.offset == 0) {
                this.firstEdgeNumber = edgeNumber;
                int edgeNumber2 = this.lessThan.markRightEdgesFirst(this.greaterOrEqual.markRightEdgesFirst(edgeNumber) - 1);
                this.offset = edgeNumber2;
                return edgeNumber2;
            }
            return edgeNumber;
        }

        @Override
        public void write(StringTrieBuilder builder) {
            this.lessThan.writeUnlessInsideRightEdge(this.firstEdgeNumber, this.greaterOrEqual.getOffset(), builder);
            this.greaterOrEqual.write(builder);
            if (!f115assertionsDisabled) {
                if (!(this.lessThan.getOffset() > 0)) {
                    throw new AssertionError();
                }
            }
            builder.writeDeltaTo(this.lessThan.getOffset());
            this.offset = builder.write(this.unit);
        }
    }

    private static final class BranchHeadNode extends ValueNode {
        private int length;
        private Node next;

        public BranchHeadNode(int len, Node subNode) {
            this.length = len;
            this.next = subNode;
        }

        @Override
        public int hashCode() {
            return ((this.length + 248302782) * 37) + this.next.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!super.equals(other)) {
                return false;
            }
            BranchHeadNode o = (BranchHeadNode) other;
            return this.length == o.length && this.next == o.next;
        }

        @Override
        public int markRightEdgesFirst(int edgeNumber) {
            if (this.offset == 0) {
                int edgeNumber2 = this.next.markRightEdgesFirst(edgeNumber);
                this.offset = edgeNumber2;
                return edgeNumber2;
            }
            return edgeNumber;
        }

        @Override
        public void write(StringTrieBuilder builder) {
            this.next.write(builder);
            if (this.length <= builder.getMinLinearMatch()) {
                this.offset = builder.writeValueAndType(this.hasValue, this.value, this.length - 1);
            } else {
                builder.write(this.length - 1);
                this.offset = builder.writeValueAndType(this.hasValue, this.value, 0);
            }
        }
    }

    private ValueNode createSuffixNode(CharSequence s, int start, int sValue) {
        ValueNode node = registerFinalValue(sValue);
        if (start < s.length()) {
            int offset = this.strings.length();
            this.strings.append(s, start, s.length());
            return new LinearMatchNode(this.strings, offset, s.length() - start, node);
        }
        return node;
    }

    private enum State {
        ADDING,
        BUILDING_FAST,
        BUILDING_SMALL,
        BUILT;

        public static State[] valuesCustom() {
            return values();
        }
    }
}
