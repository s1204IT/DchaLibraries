package android.icu.util;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class StringTrieBuilder {
    static final boolean $assertionsDisabled = false;
    private Node root;
    private State state = State.ADDING;

    @Deprecated
    protected StringBuilder strings = new StringBuilder();
    private HashMap<Node, Node> nodes = new HashMap<>();
    private ValueNode lookupFinalValueNode = new ValueNode();

    public enum Option {
        FAST,
        SMALL
    }

    private enum State {
        ADDING,
        BUILDING_FAST,
        BUILDING_SMALL,
        BUILT
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

    @Deprecated
    protected StringTrieBuilder() {
    }

    @Deprecated
    protected void addImpl(CharSequence charSequence, int i) {
        if (this.state != State.ADDING) {
            throw new IllegalStateException("Cannot add (string, value) pairs after build().");
        }
        if (charSequence.length() > 65535) {
            throw new IndexOutOfBoundsException("The maximum string length is 0xffff.");
        }
        if (this.root == null) {
            this.root = createSuffixNode(charSequence, 0, i);
        } else {
            this.root = this.root.add(this, charSequence, 0, i);
        }
    }

    @Deprecated
    protected final void buildImpl(Option option) {
        switch (this.state) {
            case ADDING:
                if (this.root == null) {
                    throw new IndexOutOfBoundsException("No (string, value) pairs were added.");
                }
                if (option == Option.FAST) {
                    this.state = State.BUILDING_FAST;
                } else {
                    this.state = State.BUILDING_SMALL;
                }
                break;
                break;
            case BUILDING_FAST:
            case BUILDING_SMALL:
                throw new IllegalStateException("Builder failed and must be clear()ed.");
            case BUILT:
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

    private final Node registerNode(Node node) {
        if (this.state == State.BUILDING_FAST) {
            return node;
        }
        Node node2 = this.nodes.get(node);
        if (node2 != null) {
            return node2;
        }
        this.nodes.put(node, node);
        return node;
    }

    private final ValueNode registerFinalValue(int i) {
        this.lookupFinalValueNode.setFinalValue(i);
        Node node = this.nodes.get(this.lookupFinalValueNode);
        if (node != null) {
            return (ValueNode) node;
        }
        ValueNode valueNode = new ValueNode(i);
        this.nodes.put(valueNode, valueNode);
        return valueNode;
    }

    private static abstract class Node {
        protected int offset = 0;

        public abstract int hashCode();

        public abstract void write(StringTrieBuilder stringTrieBuilder);

        public boolean equals(Object obj) {
            return this == obj || getClass() == obj.getClass();
        }

        public Node add(StringTrieBuilder stringTrieBuilder, CharSequence charSequence, int i, int i2) {
            return this;
        }

        public Node register(StringTrieBuilder stringTrieBuilder) {
            return this;
        }

        public int markRightEdgesFirst(int i) {
            if (this.offset == 0) {
                this.offset = i;
            }
            return i;
        }

        public final void writeUnlessInsideRightEdge(int i, int i2, StringTrieBuilder stringTrieBuilder) {
            if (this.offset < 0) {
                if (this.offset < i2 || i < this.offset) {
                    write(stringTrieBuilder);
                }
            }
        }

        public final int getOffset() {
            return this.offset;
        }
    }

    private static class ValueNode extends Node {
        static final boolean $assertionsDisabled = false;
        protected boolean hasValue;
        protected int value;

        public ValueNode() {
        }

        public ValueNode(int i) {
            this.hasValue = true;
            this.value = i;
        }

        public final void setValue(int i) {
            this.hasValue = true;
            this.value = i;
        }

        private void setFinalValue(int i) {
            this.hasValue = true;
            this.value = i;
        }

        @Override
        public int hashCode() {
            if (this.hasValue) {
                return 41383797 + this.value;
            }
            return 1118481;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj)) {
                return false;
            }
            ValueNode valueNode = (ValueNode) obj;
            return this.hasValue == valueNode.hasValue && (!this.hasValue || this.value == valueNode.value);
        }

        @Override
        public Node add(StringTrieBuilder stringTrieBuilder, CharSequence charSequence, int i, int i2) {
            if (i != charSequence.length()) {
                ValueNode valueNodeCreateSuffixNode = stringTrieBuilder.createSuffixNode(charSequence, i, i2);
                valueNodeCreateSuffixNode.setValue(this.value);
                return valueNodeCreateSuffixNode;
            }
            throw new IllegalArgumentException("Duplicate string.");
        }

        @Override
        public void write(StringTrieBuilder stringTrieBuilder) {
            this.offset = stringTrieBuilder.writeValueAndFinal(this.value, true);
        }
    }

    private static final class IntermediateValueNode extends ValueNode {
        private Node next;

        public IntermediateValueNode(int i, Node node) {
            this.next = node;
            setValue(i);
        }

        @Override
        public int hashCode() {
            return ((82767594 + this.value) * 37) + this.next.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            return super.equals(obj) && this.next == ((IntermediateValueNode) obj).next;
        }

        @Override
        public int markRightEdgesFirst(int i) {
            if (this.offset == 0) {
                int iMarkRightEdgesFirst = this.next.markRightEdgesFirst(i);
                this.offset = iMarkRightEdgesFirst;
                return iMarkRightEdgesFirst;
            }
            return i;
        }

        @Override
        public void write(StringTrieBuilder stringTrieBuilder) {
            this.next.write(stringTrieBuilder);
            this.offset = stringTrieBuilder.writeValueAndFinal(this.value, false);
        }
    }

    private static final class LinearMatchNode extends ValueNode {
        private int hash;
        private int length;
        private Node next;
        private int stringOffset;
        private CharSequence strings;

        public LinearMatchNode(CharSequence charSequence, int i, int i2, Node node) {
            this.strings = charSequence;
            this.stringOffset = i;
            this.length = i2;
            this.next = node;
        }

        @Override
        public int hashCode() {
            return this.hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj)) {
                return false;
            }
            LinearMatchNode linearMatchNode = (LinearMatchNode) obj;
            if (this.length != linearMatchNode.length || this.next != linearMatchNode.next) {
                return false;
            }
            int i = this.stringOffset;
            int i2 = linearMatchNode.stringOffset;
            int i3 = this.stringOffset + this.length;
            while (i < i3) {
                if (this.strings.charAt(i) != this.strings.charAt(i2)) {
                    return false;
                }
                i++;
                i2++;
            }
            return true;
        }

        @Override
        public Node add(StringTrieBuilder stringTrieBuilder, CharSequence charSequence, int i, int i2) {
            Node node;
            Node node2;
            if (i == charSequence.length()) {
                if (this.hasValue) {
                    throw new IllegalArgumentException("Duplicate string.");
                }
                setValue(i2);
                return this;
            }
            int i3 = this.stringOffset + this.length;
            int i4 = this.stringOffset;
            while (i4 < i3) {
                if (i == charSequence.length()) {
                    int i5 = i4 - this.stringOffset;
                    LinearMatchNode linearMatchNode = new LinearMatchNode(this.strings, i4, this.length - i5, this.next);
                    linearMatchNode.setValue(i2);
                    this.length = i5;
                    this.next = linearMatchNode;
                    return this;
                }
                char cCharAt = this.strings.charAt(i4);
                char cCharAt2 = charSequence.charAt(i);
                if (cCharAt == cCharAt2) {
                    i4++;
                    i++;
                } else {
                    DynamicBranchNode dynamicBranchNode = new DynamicBranchNode();
                    if (i4 == this.stringOffset) {
                        if (this.hasValue) {
                            dynamicBranchNode.setValue(this.value);
                            this.value = 0;
                            this.hasValue = false;
                        }
                        this.stringOffset++;
                        this.length--;
                        if (this.length <= 0) {
                            node2 = this.next;
                        } else {
                            node2 = this;
                        }
                        node = dynamicBranchNode;
                    } else if (i4 == i3 - 1) {
                        this.length--;
                        node2 = this.next;
                        this.next = dynamicBranchNode;
                        node = this;
                    } else {
                        int i6 = i4 - this.stringOffset;
                        LinearMatchNode linearMatchNode2 = new LinearMatchNode(this.strings, i4 + 1, this.length - (i6 + 1), this.next);
                        this.length = i6;
                        this.next = dynamicBranchNode;
                        node = this;
                        node2 = linearMatchNode2;
                    }
                    ValueNode valueNodeCreateSuffixNode = stringTrieBuilder.createSuffixNode(charSequence, i + 1, i2);
                    dynamicBranchNode.add(cCharAt, node2);
                    dynamicBranchNode.add(cCharAt2, valueNodeCreateSuffixNode);
                    return node;
                }
            }
            this.next = this.next.add(stringTrieBuilder, charSequence, i, i2);
            return this;
        }

        @Override
        public Node register(StringTrieBuilder stringTrieBuilder) {
            Node intermediateValueNode;
            this.next = this.next.register(stringTrieBuilder);
            int maxLinearMatchLength = stringTrieBuilder.getMaxLinearMatchLength();
            while (this.length > maxLinearMatchLength) {
                int i = (this.stringOffset + this.length) - maxLinearMatchLength;
                this.length -= maxLinearMatchLength;
                LinearMatchNode linearMatchNode = new LinearMatchNode(this.strings, i, maxLinearMatchLength, this.next);
                linearMatchNode.setHashCode();
                this.next = stringTrieBuilder.registerNode(linearMatchNode);
            }
            if (this.hasValue && !stringTrieBuilder.matchNodesCanHaveValues()) {
                int i2 = this.value;
                this.value = 0;
                this.hasValue = false;
                setHashCode();
                intermediateValueNode = new IntermediateValueNode(i2, stringTrieBuilder.registerNode(this));
            } else {
                setHashCode();
                intermediateValueNode = this;
            }
            return stringTrieBuilder.registerNode(intermediateValueNode);
        }

        @Override
        public int markRightEdgesFirst(int i) {
            if (this.offset == 0) {
                int iMarkRightEdgesFirst = this.next.markRightEdgesFirst(i);
                this.offset = iMarkRightEdgesFirst;
                return iMarkRightEdgesFirst;
            }
            return i;
        }

        @Override
        public void write(StringTrieBuilder stringTrieBuilder) {
            this.next.write(stringTrieBuilder);
            stringTrieBuilder.write(this.stringOffset, this.length);
            this.offset = stringTrieBuilder.writeValueAndType(this.hasValue, this.value, (stringTrieBuilder.getMinLinearMatch() + this.length) - 1);
        }

        private void setHashCode() {
            this.hash = ((124151391 + this.length) * 37) + this.next.hashCode();
            if (this.hasValue) {
                this.hash = (this.hash * 37) + this.value;
            }
            int i = this.stringOffset + this.length;
            for (int i2 = this.stringOffset; i2 < i; i2++) {
                this.hash = (this.hash * 37) + this.strings.charAt(i2);
            }
        }
    }

    private static final class DynamicBranchNode extends ValueNode {
        private StringBuilder chars = new StringBuilder();
        private ArrayList<Node> equal = new ArrayList<>();

        public void add(char c, Node node) {
            int iFind = find(c);
            this.chars.insert(iFind, c);
            this.equal.add(iFind, node);
        }

        @Override
        public Node add(StringTrieBuilder stringTrieBuilder, CharSequence charSequence, int i, int i2) {
            if (i == charSequence.length()) {
                if (this.hasValue) {
                    throw new IllegalArgumentException("Duplicate string.");
                }
                setValue(i2);
                return this;
            }
            int i3 = i + 1;
            char cCharAt = charSequence.charAt(i);
            int iFind = find(cCharAt);
            if (iFind < this.chars.length() && cCharAt == this.chars.charAt(iFind)) {
                this.equal.set(iFind, this.equal.get(iFind).add(stringTrieBuilder, charSequence, i3, i2));
            } else {
                this.chars.insert(iFind, cCharAt);
                this.equal.add(iFind, stringTrieBuilder.createSuffixNode(charSequence, i3, i2));
            }
            return this;
        }

        @Override
        public Node register(StringTrieBuilder stringTrieBuilder) {
            Node intermediateValueNode;
            BranchHeadNode branchHeadNode = new BranchHeadNode(this.chars.length(), register(stringTrieBuilder, 0, this.chars.length()));
            if (this.hasValue) {
                if (stringTrieBuilder.matchNodesCanHaveValues()) {
                    branchHeadNode.setValue(this.value);
                    intermediateValueNode = branchHeadNode;
                } else {
                    intermediateValueNode = new IntermediateValueNode(this.value, stringTrieBuilder.registerNode(branchHeadNode));
                }
            } else {
                intermediateValueNode = branchHeadNode;
            }
            return stringTrieBuilder.registerNode(intermediateValueNode);
        }

        private Node register(StringTrieBuilder stringTrieBuilder, int i, int i2) {
            int i3 = i2 - i;
            if (i3 > stringTrieBuilder.getMaxBranchLinearSubNodeLength()) {
                int i4 = (i3 / 2) + i;
                return stringTrieBuilder.registerNode(new SplitBranchNode(this.chars.charAt(i4), register(stringTrieBuilder, i, i4), register(stringTrieBuilder, i4, i2)));
            }
            ListBranchNode listBranchNode = new ListBranchNode(i3);
            do {
                char cCharAt = this.chars.charAt(i);
                Node node = this.equal.get(i);
                if (node.getClass() == ValueNode.class) {
                    listBranchNode.add(cCharAt, ((ValueNode) node).value);
                } else {
                    listBranchNode.add(cCharAt, node.register(stringTrieBuilder));
                }
                i++;
            } while (i < i2);
            return stringTrieBuilder.registerNode(listBranchNode);
        }

        private int find(char c) {
            int length = this.chars.length();
            int i = 0;
            while (i < length) {
                int i2 = (i + length) / 2;
                char cCharAt = this.chars.charAt(i2);
                if (c >= cCharAt) {
                    if (c == cCharAt) {
                        return i2;
                    }
                    i = i2 + 1;
                } else {
                    length = i2;
                }
            }
            return i;
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
        static final boolean $assertionsDisabled = false;
        private Node[] equal;
        private int length;
        private char[] units;
        private int[] values;

        public ListBranchNode(int i) {
            this.hash = 165535188 + i;
            this.equal = new Node[i];
            this.values = new int[i];
            this.units = new char[i];
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj)) {
                return false;
            }
            ListBranchNode listBranchNode = (ListBranchNode) obj;
            for (int i = 0; i < this.length; i++) {
                if (this.units[i] != listBranchNode.units[i] || this.values[i] != listBranchNode.values[i] || this.equal[i] != listBranchNode.equal[i]) {
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
        public int markRightEdgesFirst(int i) {
            if (this.offset == 0) {
                this.firstEdgeNumber = i;
                int i2 = 0;
                int i3 = this.length;
                do {
                    i3--;
                    Node node = this.equal[i3];
                    if (node != null) {
                        i = node.markRightEdgesFirst(i - i2);
                    }
                    i2 = 1;
                } while (i3 > 0);
                this.offset = i;
            }
            return i;
        }

        @Override
        public void write(StringTrieBuilder stringTrieBuilder) {
            int offset;
            boolean z;
            int i = this.length - 1;
            Node node = this.equal[i];
            int offset2 = node == null ? this.firstEdgeNumber : node.getOffset();
            do {
                i--;
                if (this.equal[i] != null) {
                    this.equal[i].writeUnlessInsideRightEdge(this.firstEdgeNumber, offset2, stringTrieBuilder);
                }
            } while (i > 0);
            int i2 = this.length - 1;
            if (node == null) {
                stringTrieBuilder.writeValueAndFinal(this.values[i2], true);
            } else {
                node.write(stringTrieBuilder);
            }
            this.offset = stringTrieBuilder.write(this.units[i2]);
            while (true) {
                i2--;
                if (i2 >= 0) {
                    if (this.equal[i2] == null) {
                        offset = this.values[i2];
                        z = true;
                    } else {
                        offset = this.offset - this.equal[i2].getOffset();
                        z = false;
                    }
                    stringTrieBuilder.writeValueAndFinal(offset, z);
                    this.offset = stringTrieBuilder.write(this.units[i2]);
                } else {
                    return;
                }
            }
        }

        public void add(int i, int i2) {
            this.units[this.length] = (char) i;
            this.equal[this.length] = null;
            this.values[this.length] = i2;
            this.length++;
            this.hash = (((this.hash * 37) + i) * 37) + i2;
        }

        public void add(int i, Node node) {
            this.units[this.length] = (char) i;
            this.equal[this.length] = node;
            this.values[this.length] = 0;
            this.length++;
            this.hash = (((this.hash * 37) + i) * 37) + node.hashCode();
        }
    }

    private static final class SplitBranchNode extends BranchNode {
        static final boolean $assertionsDisabled = false;
        private Node greaterOrEqual;
        private Node lessThan;
        private char unit;

        public SplitBranchNode(char c, Node node, Node node2) {
            this.hash = ((((206918985 + c) * 37) + node.hashCode()) * 37) + node2.hashCode();
            this.unit = c;
            this.lessThan = node;
            this.greaterOrEqual = node2;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj)) {
                return false;
            }
            SplitBranchNode splitBranchNode = (SplitBranchNode) obj;
            return this.unit == splitBranchNode.unit && this.lessThan == splitBranchNode.lessThan && this.greaterOrEqual == splitBranchNode.greaterOrEqual;
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public int markRightEdgesFirst(int i) {
            if (this.offset == 0) {
                this.firstEdgeNumber = i;
                int iMarkRightEdgesFirst = this.lessThan.markRightEdgesFirst(this.greaterOrEqual.markRightEdgesFirst(i) - 1);
                this.offset = iMarkRightEdgesFirst;
                return iMarkRightEdgesFirst;
            }
            return i;
        }

        @Override
        public void write(StringTrieBuilder stringTrieBuilder) {
            this.lessThan.writeUnlessInsideRightEdge(this.firstEdgeNumber, this.greaterOrEqual.getOffset(), stringTrieBuilder);
            this.greaterOrEqual.write(stringTrieBuilder);
            stringTrieBuilder.writeDeltaTo(this.lessThan.getOffset());
            this.offset = stringTrieBuilder.write(this.unit);
        }
    }

    private static final class BranchHeadNode extends ValueNode {
        private int length;
        private Node next;

        public BranchHeadNode(int i, Node node) {
            this.length = i;
            this.next = node;
        }

        @Override
        public int hashCode() {
            return ((248302782 + this.length) * 37) + this.next.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj)) {
                return false;
            }
            BranchHeadNode branchHeadNode = (BranchHeadNode) obj;
            return this.length == branchHeadNode.length && this.next == branchHeadNode.next;
        }

        @Override
        public int markRightEdgesFirst(int i) {
            if (this.offset == 0) {
                int iMarkRightEdgesFirst = this.next.markRightEdgesFirst(i);
                this.offset = iMarkRightEdgesFirst;
                return iMarkRightEdgesFirst;
            }
            return i;
        }

        @Override
        public void write(StringTrieBuilder stringTrieBuilder) {
            this.next.write(stringTrieBuilder);
            if (this.length <= stringTrieBuilder.getMinLinearMatch()) {
                this.offset = stringTrieBuilder.writeValueAndType(this.hasValue, this.value, this.length - 1);
            } else {
                stringTrieBuilder.write(this.length - 1);
                this.offset = stringTrieBuilder.writeValueAndType(this.hasValue, this.value, 0);
            }
        }
    }

    private ValueNode createSuffixNode(CharSequence charSequence, int i, int i2) {
        ValueNode valueNodeRegisterFinalValue = registerFinalValue(i2);
        if (i >= charSequence.length()) {
            return valueNodeRegisterFinalValue;
        }
        int length = this.strings.length();
        this.strings.append(charSequence, i, charSequence.length());
        return new LinearMatchNode(this.strings, length, charSequence.length() - i, valueNodeRegisterFinalValue);
    }
}
