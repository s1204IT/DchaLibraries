package android.icu.text;

import android.icu.text.MessagePattern;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MessagePatternUtil {
    private MessagePatternUtil() {
    }

    public static MessageNode buildMessageNode(String str) {
        return buildMessageNode(new MessagePattern(str));
    }

    public static MessageNode buildMessageNode(MessagePattern messagePattern) {
        int iCountParts = messagePattern.countParts() - 1;
        if (iCountParts < 0) {
            throw new IllegalArgumentException("The MessagePattern is empty");
        }
        if (messagePattern.getPartType(0) != MessagePattern.Part.Type.MSG_START) {
            throw new IllegalArgumentException("The MessagePattern does not represent a MessageFormat pattern");
        }
        return buildMessageNode(messagePattern, 0, iCountParts);
    }

    public static class Node {
        private Node() {
        }
    }

    public static class MessageNode extends Node {
        private volatile List<MessageContentsNode> list;

        public List<MessageContentsNode> getContents() {
            return this.list;
        }

        public String toString() {
            return this.list.toString();
        }

        private MessageNode() {
            super();
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

        public enum Type {
            TEXT,
            ARG,
            REPLACE_NUMBER
        }

        public Type getType() {
            return this.type;
        }

        public String toString() {
            return "{REPLACE_NUMBER}";
        }

        private MessageContentsNode(Type type) {
            super();
            this.type = type;
        }

        private static MessageContentsNode createReplaceNumberNode() {
            return new MessageContentsNode(Type.REPLACE_NUMBER);
        }
    }

    public static class TextNode extends MessageContentsNode {
        private String text;

        public String getText() {
            return this.text;
        }

        @Override
        public String toString() {
            return "«" + this.text + "»";
        }

        private TextNode(String str) {
            super(MessageContentsNode.Type.TEXT);
            this.text = str;
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
            sb.append('{');
            sb.append(this.name);
            if (this.argType != MessagePattern.ArgType.NONE) {
                sb.append(',');
                sb.append(this.typeName);
                if (this.argType == MessagePattern.ArgType.SIMPLE) {
                    if (this.style != null) {
                        sb.append(',');
                        sb.append(this.style);
                    }
                } else {
                    sb.append(',');
                    sb.append(this.complexStyle.toString());
                }
            }
            sb.append('}');
            return sb.toString();
        }

        private ArgNode() {
            super(MessageContentsNode.Type.ARG);
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

        public VariantNode getVariantsByType(List<VariantNode> list, List<VariantNode> list2) {
            if (list != null) {
                list.clear();
            }
            list2.clear();
            VariantNode variantNode = null;
            for (VariantNode variantNode2 : this.list) {
                if (variantNode2.isSelectorNumeric()) {
                    list.add(variantNode2);
                } else if (PluralRules.KEYWORD_OTHER.equals(variantNode2.getSelector())) {
                    if (variantNode == null) {
                        variantNode = variantNode2;
                    }
                } else {
                    list2.add(variantNode2);
                }
            }
            return variantNode;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('(');
            sb.append(this.argType.toString());
            sb.append(" style) ");
            if (hasExplicitOffset()) {
                sb.append("offset:");
                sb.append(this.offset);
                sb.append(' ');
            }
            sb.append(this.list.toString());
            return sb.toString();
        }

        private ComplexArgStyleNode(MessagePattern.ArgType argType) {
            super();
            this.list = new ArrayList();
            this.argType = argType;
        }

        private void addVariant(VariantNode variantNode) {
            this.list.add(variantNode);
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
                sb.append(this.numericValue);
                sb.append(" (");
                sb.append(this.selector);
                sb.append(") {");
            } else {
                sb.append(this.selector);
                sb.append(" {");
            }
            sb.append(this.msgNode.toString());
            sb.append('}');
            return sb.toString();
        }

        private VariantNode() {
            super();
            this.numericValue = -1.23456789E8d;
        }
    }

    private static MessageNode buildMessageNode(MessagePattern messagePattern, int i, int i2) {
        int limit = messagePattern.getPart(i).getLimit();
        MessageNode messageNode = new MessageNode();
        while (true) {
            i++;
            MessagePattern.Part part = messagePattern.getPart(i);
            int index = part.getIndex();
            if (limit < index) {
                messageNode.addContentsNode(new TextNode(messagePattern.getPatternString().substring(limit, index)));
            }
            if (i == i2) {
                return messageNode.freeze();
            }
            MessagePattern.Part.Type type = part.getType();
            if (type == MessagePattern.Part.Type.ARG_START) {
                int limitPartIndex = messagePattern.getLimitPartIndex(i);
                messageNode.addContentsNode(buildArgNode(messagePattern, i, limitPartIndex));
                part = messagePattern.getPart(limitPartIndex);
                i = limitPartIndex;
            } else if (type == MessagePattern.Part.Type.REPLACE_NUMBER) {
                messageNode.addContentsNode(MessageContentsNode.createReplaceNumberNode());
            }
            limit = part.getLimit();
        }
    }

    private static ArgNode buildArgNode(MessagePattern messagePattern, int i, int i2) {
        ArgNode argNodeCreateArgNode = ArgNode.createArgNode();
        MessagePattern.ArgType argType = argNodeCreateArgNode.argType = messagePattern.getPart(i).getArgType();
        int i3 = i + 1;
        MessagePattern.Part part = messagePattern.getPart(i3);
        argNodeCreateArgNode.name = messagePattern.getSubstring(part);
        if (part.getType() == MessagePattern.Part.Type.ARG_NUMBER) {
            argNodeCreateArgNode.number = part.getValue();
        }
        int i4 = i3 + 1;
        switch (argType) {
            case SIMPLE:
                int i5 = i4 + 1;
                argNodeCreateArgNode.typeName = messagePattern.getSubstring(messagePattern.getPart(i4));
                if (i5 < i2) {
                    argNodeCreateArgNode.style = messagePattern.getSubstring(messagePattern.getPart(i5));
                }
                return argNodeCreateArgNode;
            case CHOICE:
                argNodeCreateArgNode.typeName = "choice";
                argNodeCreateArgNode.complexStyle = buildChoiceStyleNode(messagePattern, i4, i2);
                return argNodeCreateArgNode;
            case PLURAL:
                argNodeCreateArgNode.typeName = "plural";
                argNodeCreateArgNode.complexStyle = buildPluralStyleNode(messagePattern, i4, i2, argType);
                return argNodeCreateArgNode;
            case SELECT:
                argNodeCreateArgNode.typeName = "select";
                argNodeCreateArgNode.complexStyle = buildSelectStyleNode(messagePattern, i4, i2);
                return argNodeCreateArgNode;
            case SELECTORDINAL:
                argNodeCreateArgNode.typeName = "selectordinal";
                argNodeCreateArgNode.complexStyle = buildPluralStyleNode(messagePattern, i4, i2, argType);
                return argNodeCreateArgNode;
            default:
                return argNodeCreateArgNode;
        }
    }

    private static ComplexArgStyleNode buildChoiceStyleNode(MessagePattern messagePattern, int i, int i2) {
        ComplexArgStyleNode complexArgStyleNode = new ComplexArgStyleNode(MessagePattern.ArgType.CHOICE);
        while (i < i2) {
            double numericValue = messagePattern.getNumericValue(messagePattern.getPart(i));
            int i3 = i + 2;
            int limitPartIndex = messagePattern.getLimitPartIndex(i3);
            VariantNode variantNode = new VariantNode();
            variantNode.selector = messagePattern.getSubstring(messagePattern.getPart(i + 1));
            variantNode.numericValue = numericValue;
            variantNode.msgNode = buildMessageNode(messagePattern, i3, limitPartIndex);
            complexArgStyleNode.addVariant(variantNode);
            i = limitPartIndex + 1;
        }
        return complexArgStyleNode.freeze();
    }

    private static ComplexArgStyleNode buildPluralStyleNode(MessagePattern messagePattern, int i, int i2, MessagePattern.ArgType argType) {
        ComplexArgStyleNode complexArgStyleNode = new ComplexArgStyleNode(argType);
        MessagePattern.Part part = messagePattern.getPart(i);
        if (part.getType().hasNumericValue()) {
            complexArgStyleNode.explicitOffset = true;
            complexArgStyleNode.offset = messagePattern.getNumericValue(part);
            i++;
        }
        while (i < i2) {
            int i3 = i + 1;
            MessagePattern.Part part2 = messagePattern.getPart(i);
            double numericValue = -1.23456789E8d;
            MessagePattern.Part part3 = messagePattern.getPart(i3);
            if (part3.getType().hasNumericValue()) {
                numericValue = messagePattern.getNumericValue(part3);
                i3++;
            }
            int limitPartIndex = messagePattern.getLimitPartIndex(i3);
            VariantNode variantNode = new VariantNode();
            variantNode.selector = messagePattern.getSubstring(part2);
            variantNode.numericValue = numericValue;
            variantNode.msgNode = buildMessageNode(messagePattern, i3, limitPartIndex);
            complexArgStyleNode.addVariant(variantNode);
            i = limitPartIndex + 1;
        }
        return complexArgStyleNode.freeze();
    }

    private static ComplexArgStyleNode buildSelectStyleNode(MessagePattern messagePattern, int i, int i2) {
        ComplexArgStyleNode complexArgStyleNode = new ComplexArgStyleNode(MessagePattern.ArgType.SELECT);
        while (i < i2) {
            int i3 = i + 1;
            MessagePattern.Part part = messagePattern.getPart(i);
            int limitPartIndex = messagePattern.getLimitPartIndex(i3);
            VariantNode variantNode = new VariantNode();
            variantNode.selector = messagePattern.getSubstring(part);
            variantNode.msgNode = buildMessageNode(messagePattern, i3, limitPartIndex);
            complexArgStyleNode.addVariant(variantNode);
            i = limitPartIndex + 1;
        }
        return complexArgStyleNode.freeze();
    }
}
