package android.icu.text;

import android.icu.text.MessagePattern;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MessagePatternUtil {

    private static final int[] f74androidicutextMessagePattern$ArgTypeSwitchesValues = null;

    private static int[] m146getandroidicutextMessagePattern$ArgTypeSwitchesValues() {
        if (f74androidicutextMessagePattern$ArgTypeSwitchesValues != null) {
            return f74androidicutextMessagePattern$ArgTypeSwitchesValues;
        }
        int[] iArr = new int[MessagePattern.ArgType.valuesCustom().length];
        try {
            iArr[MessagePattern.ArgType.CHOICE.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[MessagePattern.ArgType.NONE.ordinal()] = 6;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[MessagePattern.ArgType.PLURAL.ordinal()] = 2;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[MessagePattern.ArgType.SELECT.ordinal()] = 3;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[MessagePattern.ArgType.SELECTORDINAL.ordinal()] = 4;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[MessagePattern.ArgType.SIMPLE.ordinal()] = 5;
        } catch (NoSuchFieldError e6) {
        }
        f74androidicutextMessagePattern$ArgTypeSwitchesValues = iArr;
        return iArr;
    }

    private MessagePatternUtil() {
    }

    public static MessageNode buildMessageNode(String patternString) {
        return buildMessageNode(new MessagePattern(patternString));
    }

    public static MessageNode buildMessageNode(MessagePattern pattern) {
        int limit = pattern.countParts() - 1;
        if (limit < 0) {
            throw new IllegalArgumentException("The MessagePattern is empty");
        }
        if (pattern.getPartType(0) != MessagePattern.Part.Type.MSG_START) {
            throw new IllegalArgumentException("The MessagePattern does not represent a MessageFormat pattern");
        }
        return buildMessageNode(pattern, 0, limit);
    }

    public static class Node {
        Node(Node node) {
            this();
        }

        private Node() {
        }
    }

    public static class MessageNode extends Node {
        private volatile List<MessageContentsNode> list;

        MessageNode(MessageNode messageNode) {
            this();
        }

        public List<MessageContentsNode> getContents() {
            return this.list;
        }

        public String toString() {
            return this.list.toString();
        }

        private MessageNode() {
            super(null);
            this.list = new ArrayList();
        }

        private void addContentsNode(MessageContentsNode messageContentsNode) {
            if ((messageContentsNode instanceof TextNode) && !this.list.isEmpty()) {
                MessageContentsNode messageContentsNode2 = this.list.get(this.list.size() - 1);
                if (messageContentsNode2 instanceof TextNode) {
                    ((TextNode) messageContentsNode2).text += ((TextNode) messageContentsNode).text;
                    return;
                }
            }
            this.list.add(messageContentsNode);
        }

        private MessageNode freeze() {
            this.list = Collections.unmodifiableList(this.list);
            return this;
        }
    }

    public static class MessageContentsNode extends Node {
        private Type type;

        MessageContentsNode(Type type, MessageContentsNode messageContentsNode) {
            this(type);
        }

        public enum Type {
            TEXT,
            ARG,
            REPLACE_NUMBER;

            public static Type[] valuesCustom() {
                return values();
            }
        }

        public Type getType() {
            return this.type;
        }

        public String toString() {
            return "{REPLACE_NUMBER}";
        }

        private MessageContentsNode(Type type) {
            super(null);
            this.type = type;
        }

        private static MessageContentsNode createReplaceNumberNode() {
            return new MessageContentsNode(Type.REPLACE_NUMBER);
        }
    }

    public static class TextNode extends MessageContentsNode {
        private String text;

        TextNode(String text, TextNode textNode) {
            this(text);
        }

        public String getText() {
            return this.text;
        }

        @Override
        public String toString() {
            return "«" + this.text + "»";
        }

        private TextNode(String text) {
            super(MessageContentsNode.Type.TEXT, null);
            this.text = text;
        }
    }

    public static class ArgNode extends MessageContentsNode {
        private MessagePattern.ArgType argType;
        private ComplexArgStyleNode complexStyle;
        private String name;
        private int number;
        private String style;
        private String typeName;

        public MessagePattern.ArgType getArgType() {
            return this.argType;
        }

        public String getName() {
            return this.name;
        }

        public int getNumber() {
            return this.number;
        }

        public String getTypeName() {
            return this.typeName;
        }

        public String getSimpleStyle() {
            return this.style;
        }

        public ComplexArgStyleNode getComplexStyle() {
            return this.complexStyle;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('{').append(this.name);
            if (this.argType != MessagePattern.ArgType.NONE) {
                sb.append(',').append(this.typeName);
                if (this.argType == MessagePattern.ArgType.SIMPLE) {
                    if (this.style != null) {
                        sb.append(',').append(this.style);
                    }
                } else {
                    sb.append(',').append(this.complexStyle.toString());
                }
            }
            return sb.append('}').toString();
        }

        private ArgNode() {
            super(MessageContentsNode.Type.ARG, null);
            this.number = -1;
        }

        private static ArgNode createArgNode() {
            return new ArgNode();
        }
    }

    public static class ComplexArgStyleNode extends Node {
        private MessagePattern.ArgType argType;
        private boolean explicitOffset;
        private volatile List<VariantNode> list;
        private double offset;

        ComplexArgStyleNode(MessagePattern.ArgType argType, ComplexArgStyleNode complexArgStyleNode) {
            this(argType);
        }

        public MessagePattern.ArgType getArgType() {
            return this.argType;
        }

        public boolean hasExplicitOffset() {
            return this.explicitOffset;
        }

        public double getOffset() {
            return this.offset;
        }

        public List<VariantNode> getVariants() {
            return this.list;
        }

        public VariantNode getVariantsByType(List<VariantNode> numericVariants, List<VariantNode> keywordVariants) {
            if (numericVariants != null) {
                numericVariants.clear();
            }
            keywordVariants.clear();
            VariantNode other = null;
            for (VariantNode variant : this.list) {
                if (variant.isSelectorNumeric()) {
                    numericVariants.add(variant);
                } else if (PluralRules.KEYWORD_OTHER.equals(variant.getSelector())) {
                    if (other == null) {
                        other = variant;
                    }
                } else {
                    keywordVariants.add(variant);
                }
            }
            return other;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('(').append(this.argType.toString()).append(" style) ");
            if (hasExplicitOffset()) {
                sb.append("offset:").append(this.offset).append(' ');
            }
            return sb.append(this.list.toString()).toString();
        }

        private ComplexArgStyleNode(MessagePattern.ArgType argType) {
            super(null);
            this.list = new ArrayList();
            this.argType = argType;
        }

        private void addVariant(VariantNode variant) {
            this.list.add(variant);
        }

        private ComplexArgStyleNode freeze() {
            this.list = Collections.unmodifiableList(this.list);
            return this;
        }
    }

    public static class VariantNode extends Node {
        private MessageNode msgNode;
        private double numericValue;
        private String selector;

        VariantNode(VariantNode variantNode) {
            this();
        }

        public String getSelector() {
            return this.selector;
        }

        public boolean isSelectorNumeric() {
            return this.numericValue != -1.23456789E8d;
        }

        public double getSelectorValue() {
            return this.numericValue;
        }

        public MessageNode getMessage() {
            return this.msgNode;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (isSelectorNumeric()) {
                sb.append(this.numericValue).append(" (").append(this.selector).append(") {");
            } else {
                sb.append(this.selector).append(" {");
            }
            return sb.append(this.msgNode.toString()).append('}').toString();
        }

        private VariantNode() {
            super(null);
            this.numericValue = -1.23456789E8d;
        }
    }

    private static MessageNode buildMessageNode(MessagePattern messagePattern, int i, int i2) {
        Object[] objArr = 0;
        int limit = messagePattern.getPart(i).getLimit();
        MessageNode messageNode = new MessageNode(null);
        int i3 = i + 1;
        while (true) {
            MessagePattern.Part part = messagePattern.getPart(i3);
            int index = part.getIndex();
            if (limit < index) {
                messageNode.addContentsNode(new TextNode(messagePattern.getPatternString().substring(limit, index), objArr == true ? 1 : 0));
            }
            if (i3 != i2) {
                MessagePattern.Part.Type type = part.getType();
                if (type == MessagePattern.Part.Type.ARG_START) {
                    int limitPartIndex = messagePattern.getLimitPartIndex(i3);
                    messageNode.addContentsNode(buildArgNode(messagePattern, i3, limitPartIndex));
                    i3 = limitPartIndex;
                    part = messagePattern.getPart(limitPartIndex);
                } else if (type == MessagePattern.Part.Type.REPLACE_NUMBER) {
                    messageNode.addContentsNode(MessageContentsNode.createReplaceNumberNode());
                }
                limit = part.getLimit();
                i3++;
            } else {
                return messageNode.freeze();
            }
        }
    }

    private static ArgNode buildArgNode(MessagePattern pattern, int start, int limit) {
        ArgNode node = ArgNode.createArgNode();
        MessagePattern.ArgType argType = node.argType = pattern.getPart(start).getArgType();
        int start2 = start + 1;
        MessagePattern.Part part = pattern.getPart(start2);
        node.name = pattern.getSubstring(part);
        if (part.getType() == MessagePattern.Part.Type.ARG_NUMBER) {
            node.number = part.getValue();
        }
        int start3 = start2 + 1;
        switch (m146getandroidicutextMessagePattern$ArgTypeSwitchesValues()[argType.ordinal()]) {
            case 1:
                node.typeName = "choice";
                node.complexStyle = buildChoiceStyleNode(pattern, start3, limit);
                return node;
            case 2:
                node.typeName = "plural";
                node.complexStyle = buildPluralStyleNode(pattern, start3, limit, argType);
                return node;
            case 3:
                node.typeName = "select";
                node.complexStyle = buildSelectStyleNode(pattern, start3, limit);
                return node;
            case 4:
                node.typeName = "selectordinal";
                node.complexStyle = buildPluralStyleNode(pattern, start3, limit, argType);
                return node;
            case 5:
                int start4 = start3 + 1;
                node.typeName = pattern.getSubstring(pattern.getPart(start3));
                if (start4 < limit) {
                    node.style = pattern.getSubstring(pattern.getPart(start4));
                }
                return node;
            default:
                return node;
        }
    }

    private static ComplexArgStyleNode buildChoiceStyleNode(MessagePattern messagePattern, int i, int i2) {
        Object[] objArr = 0;
        ComplexArgStyleNode complexArgStyleNode = new ComplexArgStyleNode(MessagePattern.ArgType.CHOICE, null);
        while (i < i2) {
            int i3 = i;
            double numericValue = messagePattern.getNumericValue(messagePattern.getPart(i));
            int i4 = i + 2;
            int limitPartIndex = messagePattern.getLimitPartIndex(i4);
            VariantNode variantNode = new VariantNode(objArr == true ? 1 : 0);
            variantNode.selector = messagePattern.getSubstring(messagePattern.getPart(i3 + 1));
            variantNode.numericValue = numericValue;
            variantNode.msgNode = buildMessageNode(messagePattern, i4, limitPartIndex);
            complexArgStyleNode.addVariant(variantNode);
            i = limitPartIndex + 1;
        }
        return complexArgStyleNode.freeze();
    }

    private static ComplexArgStyleNode buildPluralStyleNode(MessagePattern pattern, int start, int limit, MessagePattern.ArgType argType) {
        int start2;
        ComplexArgStyleNode node = new ComplexArgStyleNode(argType, null);
        MessagePattern.Part offset = pattern.getPart(start);
        if (offset.getType().hasNumericValue()) {
            node.explicitOffset = true;
            node.offset = pattern.getNumericValue(offset);
            start2 = start + 1;
        } else {
            start2 = start;
        }
        while (start2 < limit) {
            int start3 = start2 + 1;
            MessagePattern.Part selector = pattern.getPart(start2);
            double value = -1.23456789E8d;
            MessagePattern.Part part = pattern.getPart(start3);
            if (part.getType().hasNumericValue()) {
                value = pattern.getNumericValue(part);
                start3++;
            }
            int msgLimit = pattern.getLimitPartIndex(start3);
            VariantNode variant = new VariantNode(null);
            variant.selector = pattern.getSubstring(selector);
            variant.numericValue = value;
            variant.msgNode = buildMessageNode(pattern, start3, msgLimit);
            node.addVariant(variant);
            start2 = msgLimit + 1;
        }
        return node.freeze();
    }

    private static ComplexArgStyleNode buildSelectStyleNode(MessagePattern messagePattern, int i, int i2) {
        Object[] objArr = 0;
        ComplexArgStyleNode complexArgStyleNode = new ComplexArgStyleNode(MessagePattern.ArgType.SELECT, null);
        int i3 = i;
        while (i3 < i2) {
            int i4 = i3 + 1;
            MessagePattern.Part part = messagePattern.getPart(i3);
            int limitPartIndex = messagePattern.getLimitPartIndex(i4);
            VariantNode variantNode = new VariantNode(objArr == true ? 1 : 0);
            variantNode.selector = messagePattern.getSubstring(part);
            variantNode.msgNode = buildMessageNode(messagePattern, i4, limitPartIndex);
            complexArgStyleNode.addVariant(variantNode);
            i3 = limitPartIndex + 1;
        }
        return complexArgStyleNode.freeze();
    }
}
