package com.adobe.xmp.impl;

import com.adobe.xmp.XMPConst;
import com.adobe.xmp.XMPDateTime;
import com.adobe.xmp.XMPDateTimeFactory;
import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMetaFactory;
import com.adobe.xmp.XMPUtils;
import com.adobe.xmp.impl.xpath.XMPPath;
import com.adobe.xmp.impl.xpath.XMPPathSegment;
import com.adobe.xmp.options.PropertyOptions;
import java.util.GregorianCalendar;
import java.util.Iterator;

public class XMPNodeUtils implements XMPConst {
    static final boolean $assertionsDisabled;
    static final int CLT_FIRST_ITEM = 5;
    static final int CLT_MULTIPLE_GENERIC = 3;
    static final int CLT_NO_VALUES = 0;
    static final int CLT_SINGLE_GENERIC = 2;
    static final int CLT_SPECIFIC_MATCH = 1;
    static final int CLT_XDEFAULT = 4;

    static {
        $assertionsDisabled = !XMPNodeUtils.class.desiredAssertionStatus();
    }

    private XMPNodeUtils() {
    }

    static XMPNode findSchemaNode(XMPNode tree, String namespaceURI, boolean createNodes) throws XMPException {
        return findSchemaNode(tree, namespaceURI, null, createNodes);
    }

    static XMPNode findSchemaNode(XMPNode tree, String namespaceURI, String suggestedPrefix, boolean createNodes) throws XMPException {
        if (!$assertionsDisabled && tree.getParent() != null) {
            throw new AssertionError();
        }
        XMPNode schemaNode = tree.findChildByName(namespaceURI);
        if (schemaNode == null && createNodes) {
            schemaNode = new XMPNode(namespaceURI, new PropertyOptions().setSchemaNode(true));
            schemaNode.setImplicit(true);
            String prefix = XMPMetaFactory.getSchemaRegistry().getNamespacePrefix(namespaceURI);
            if (prefix == null) {
                if (suggestedPrefix != null && suggestedPrefix.length() != 0) {
                    prefix = XMPMetaFactory.getSchemaRegistry().registerNamespace(namespaceURI, suggestedPrefix);
                } else {
                    throw new XMPException("Unregistered schema namespace URI", 101);
                }
            }
            schemaNode.setValue(prefix);
            tree.addChild(schemaNode);
        }
        return schemaNode;
    }

    static XMPNode findChildNode(XMPNode parent, String childName, boolean createNodes) throws XMPException {
        if (!parent.getOptions().isSchemaNode() && !parent.getOptions().isStruct()) {
            if (!parent.isImplicit()) {
                throw new XMPException("Named children only allowed for schemas and structs", 102);
            }
            if (parent.getOptions().isArray()) {
                throw new XMPException("Named children not allowed for arrays", 102);
            }
            if (createNodes) {
                parent.getOptions().setStruct(true);
            }
        }
        XMPNode childNode = parent.findChildByName(childName);
        if (childNode == null && createNodes) {
            PropertyOptions options = new PropertyOptions();
            childNode = new XMPNode(childName, options);
            childNode.setImplicit(true);
            parent.addChild(childNode);
        }
        if (!$assertionsDisabled && childNode == null && createNodes) {
            throw new AssertionError();
        }
        return childNode;
    }

    static XMPNode findNode(XMPNode xmpTree, XMPPath xpath, boolean createNodes, PropertyOptions leafOptions) throws XMPException {
        if (xpath == null || xpath.size() == 0) {
            throw new XMPException("Empty XMPPath", 102);
        }
        XMPNode rootImplicitNode = null;
        XMPNode currNode = findSchemaNode(xmpTree, xpath.getSegment(0).getName(), createNodes);
        if (currNode == null) {
            return null;
        }
        if (currNode.isImplicit()) {
            currNode.setImplicit(false);
            rootImplicitNode = currNode;
        }
        for (int i = 1; i < xpath.size(); i++) {
            try {
                currNode = followXPathStep(currNode, xpath.getSegment(i), createNodes);
                if (currNode == null) {
                    if (!createNodes) {
                        return null;
                    }
                    deleteNode(rootImplicitNode);
                    return null;
                }
                if (currNode.isImplicit()) {
                    currNode.setImplicit(false);
                    if (i == 1 && xpath.getSegment(i).isAlias() && xpath.getSegment(i).getAliasForm() != 0) {
                        currNode.getOptions().setOption(xpath.getSegment(i).getAliasForm(), true);
                    } else if (i < xpath.size() - 1 && xpath.getSegment(i).getKind() == 1 && !currNode.getOptions().isCompositeProperty()) {
                        currNode.getOptions().setStruct(true);
                    }
                    if (rootImplicitNode == null) {
                        rootImplicitNode = currNode;
                    }
                }
            } catch (XMPException e) {
                if (rootImplicitNode != null) {
                    deleteNode(rootImplicitNode);
                }
                throw e;
            }
        }
        if (rootImplicitNode != null) {
            currNode.getOptions().mergeWith(leafOptions);
            currNode.setOptions(currNode.getOptions());
        }
        return currNode;
    }

    static void deleteNode(XMPNode node) {
        XMPNode parent = node.getParent();
        if (node.getOptions().isQualifier()) {
            parent.removeQualifier(node);
        } else {
            parent.removeChild(node);
        }
        if (!parent.hasChildren() && parent.getOptions().isSchemaNode()) {
            parent.getParent().removeChild(parent);
        }
    }

    static void setNodeValue(XMPNode node, Object value) {
        String strValue = serializeNodeValue(value);
        if (!node.getOptions().isQualifier() || !XMPConst.XML_LANG.equals(node.getName())) {
            node.setValue(strValue);
        } else {
            node.setValue(Utils.normalizeLangValue(strValue));
        }
    }

    static PropertyOptions verifySetOptions(PropertyOptions options, Object itemValue) throws XMPException {
        if (options == null) {
            options = new PropertyOptions();
        }
        if (options.isArrayAltText()) {
            options.setArrayAlternate(true);
        }
        if (options.isArrayAlternate()) {
            options.setArrayOrdered(true);
        }
        if (options.isArrayOrdered()) {
            options.setArray(true);
        }
        if (options.isCompositeProperty() && itemValue != null && itemValue.toString().length() > 0) {
            throw new XMPException("Structs and arrays can't have values", 103);
        }
        options.assertConsistency(options.getOptions());
        return options;
    }

    static String serializeNodeValue(Object value) {
        String strValue;
        if (value == null) {
            strValue = null;
        } else if (value instanceof Boolean) {
            strValue = XMPUtils.convertFromBoolean(((Boolean) value).booleanValue());
        } else if (value instanceof Integer) {
            strValue = XMPUtils.convertFromInteger(((Integer) value).intValue());
        } else if (value instanceof Long) {
            strValue = XMPUtils.convertFromLong(((Long) value).longValue());
        } else if (value instanceof Double) {
            strValue = XMPUtils.convertFromDouble(((Double) value).doubleValue());
        } else if (value instanceof XMPDateTime) {
            strValue = XMPUtils.convertFromDate((XMPDateTime) value);
        } else if (value instanceof GregorianCalendar) {
            XMPDateTime dt = XMPDateTimeFactory.createFromCalendar((GregorianCalendar) value);
            strValue = XMPUtils.convertFromDate(dt);
        } else if (value instanceof byte[]) {
            strValue = XMPUtils.encodeBase64((byte[]) value);
        } else {
            strValue = value.toString();
        }
        if (strValue != null) {
            return Utils.removeControlChars(strValue);
        }
        return null;
    }

    private static XMPNode followXPathStep(XMPNode parentNode, XMPPathSegment nextStep, boolean createNodes) throws XMPException {
        int index;
        int stepKind = nextStep.getKind();
        if (stepKind == 1) {
            XMPNode nextNode = findChildNode(parentNode, nextStep.getName(), createNodes);
            return nextNode;
        }
        if (stepKind == 2) {
            XMPNode nextNode2 = findQualifierNode(parentNode, nextStep.getName().substring(1), createNodes);
            return nextNode2;
        }
        if (!parentNode.getOptions().isArray()) {
            throw new XMPException("Indexing applied to non-array", 102);
        }
        if (stepKind == 3) {
            index = findIndexedItem(parentNode, nextStep.getName(), createNodes);
        } else if (stepKind == 4) {
            index = parentNode.getChildrenLength();
        } else if (stepKind == 6) {
            String[] result = Utils.splitNameAndValue(nextStep.getName());
            String fieldName = result[0];
            String fieldValue = result[1];
            index = lookupFieldSelector(parentNode, fieldName, fieldValue);
        } else if (stepKind == 5) {
            String[] result2 = Utils.splitNameAndValue(nextStep.getName());
            String qualName = result2[0];
            String qualValue = result2[1];
            index = lookupQualSelector(parentNode, qualName, qualValue, nextStep.getAliasForm());
        } else {
            throw new XMPException("Unknown array indexing step in FollowXPathStep", 9);
        }
        if (1 > index || index > parentNode.getChildrenLength()) {
            return null;
        }
        XMPNode nextNode3 = parentNode.getChild(index);
        return nextNode3;
    }

    private static XMPNode findQualifierNode(XMPNode parent, String qualName, boolean createNodes) throws XMPException {
        if (!$assertionsDisabled && qualName.startsWith("?")) {
            throw new AssertionError();
        }
        XMPNode qualNode = parent.findQualifierByName(qualName);
        if (qualNode == null && createNodes) {
            XMPNode qualNode2 = new XMPNode(qualName, null);
            qualNode2.setImplicit(true);
            parent.addQualifier(qualNode2);
            return qualNode2;
        }
        return qualNode;
    }

    private static int findIndexedItem(XMPNode arrayNode, String segment, boolean createNodes) throws XMPException {
        try {
            int index = Integer.parseInt(segment.substring(1, segment.length() - 1));
            if (index < 1) {
                throw new XMPException("Array index must be larger than zero", 102);
            }
            if (createNodes && index == arrayNode.getChildrenLength() + 1) {
                XMPNode newItem = new XMPNode(XMPConst.ARRAY_ITEM_NAME, null);
                newItem.setImplicit(true);
                arrayNode.addChild(newItem);
            }
            return index;
        } catch (NumberFormatException e) {
            throw new XMPException("Array index not digits.", 102);
        }
    }

    private static int lookupFieldSelector(XMPNode arrayNode, String fieldName, String fieldValue) throws XMPException {
        int result = -1;
        for (int index = 1; index <= arrayNode.getChildrenLength() && result < 0; index++) {
            XMPNode currItem = arrayNode.getChild(index);
            if (!currItem.getOptions().isStruct()) {
                throw new XMPException("Field selector must be used on array of struct", 102);
            }
            int f = 1;
            while (true) {
                if (f <= currItem.getChildrenLength()) {
                    XMPNode currField = currItem.getChild(f);
                    if (!fieldName.equals(currField.getName()) || !fieldValue.equals(currField.getValue())) {
                        f++;
                    } else {
                        result = index;
                        break;
                    }
                }
            }
        }
        return result;
    }

    private static int lookupQualSelector(XMPNode arrayNode, String qualName, String qualValue, int aliasForm) throws XMPException {
        if (XMPConst.XML_LANG.equals(qualName)) {
            int index = lookupLanguageItem(arrayNode, Utils.normalizeLangValue(qualValue));
            if (index >= 0 || (aliasForm & 4096) <= 0) {
                return index;
            }
            XMPNode langNode = new XMPNode(XMPConst.ARRAY_ITEM_NAME, null);
            XMPNode xdefault = new XMPNode(XMPConst.XML_LANG, XMPConst.X_DEFAULT, null);
            langNode.addQualifier(xdefault);
            arrayNode.addChild(1, langNode);
            return 1;
        }
        for (int index2 = 1; index2 < arrayNode.getChildrenLength(); index2++) {
            XMPNode currItem = arrayNode.getChild(index2);
            Iterator it = currItem.iterateQualifier();
            while (it.hasNext()) {
                XMPNode qualifier = (XMPNode) it.next();
                if (qualName.equals(qualifier.getName()) && qualValue.equals(qualifier.getValue())) {
                    return index2;
                }
            }
        }
        return -1;
    }

    static void normalizeLangArray(XMPNode arrayNode) {
        if (arrayNode.getOptions().isArrayAltText()) {
            for (int i = 2; i <= arrayNode.getChildrenLength(); i++) {
                XMPNode child = arrayNode.getChild(i);
                if (child.hasQualifier() && XMPConst.X_DEFAULT.equals(child.getQualifier(1).getValue())) {
                    try {
                        arrayNode.removeChild(i);
                        arrayNode.addChild(1, child);
                    } catch (XMPException e) {
                        if (!$assertionsDisabled) {
                            throw new AssertionError();
                        }
                    }
                    if (i == 2) {
                        arrayNode.getChild(2).setValue(child.getValue());
                        return;
                    }
                    return;
                }
            }
        }
    }

    static void detectAltText(XMPNode arrayNode) {
        if (arrayNode.getOptions().isArrayAlternate() && arrayNode.hasChildren()) {
            boolean isAltText = false;
            Iterator it = arrayNode.iterateChildren();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                XMPNode child = (XMPNode) it.next();
                if (child.getOptions().getHasLanguage()) {
                    isAltText = true;
                    break;
                }
            }
            if (isAltText) {
                arrayNode.getOptions().setArrayAltText(true);
                normalizeLangArray(arrayNode);
            }
        }
    }

    static void appendLangItem(XMPNode arrayNode, String itemLang, String itemValue) throws XMPException {
        XMPNode newItem = new XMPNode(XMPConst.ARRAY_ITEM_NAME, itemValue, null);
        XMPNode langQual = new XMPNode(XMPConst.XML_LANG, itemLang, null);
        newItem.addQualifier(langQual);
        if (!XMPConst.X_DEFAULT.equals(langQual.getValue())) {
            arrayNode.addChild(newItem);
        } else {
            arrayNode.addChild(1, newItem);
        }
    }

    static Object[] chooseLocalizedText(XMPNode arrayNode, String genericLang, String specificLang) throws XMPException {
        if (!arrayNode.getOptions().isArrayAltText()) {
            throw new XMPException("Localized text array is not alt-text", 102);
        }
        if (!arrayNode.hasChildren()) {
            return new Object[]{new Integer(0), null};
        }
        int foundGenericMatches = 0;
        XMPNode resultNode = null;
        XMPNode xDefault = null;
        Iterator it = arrayNode.iterateChildren();
        while (it.hasNext()) {
            XMPNode currItem = (XMPNode) it.next();
            if (currItem.getOptions().isCompositeProperty()) {
                throw new XMPException("Alt-text array item is not simple", 102);
            }
            if (!currItem.hasQualifier() || !XMPConst.XML_LANG.equals(currItem.getQualifier(1).getName())) {
                throw new XMPException("Alt-text array item has no language qualifier", 102);
            }
            String currLang = currItem.getQualifier(1).getValue();
            if (specificLang.equals(currLang)) {
                return new Object[]{new Integer(1), currItem};
            }
            if (genericLang != null && currLang.startsWith(genericLang)) {
                if (resultNode == null) {
                    resultNode = currItem;
                }
                foundGenericMatches++;
            } else if (XMPConst.X_DEFAULT.equals(currLang)) {
                xDefault = currItem;
            }
        }
        if (foundGenericMatches == 1) {
            return new Object[]{new Integer(2), resultNode};
        }
        if (foundGenericMatches > 1) {
            return new Object[]{new Integer(3), resultNode};
        }
        if (xDefault != null) {
            return new Object[]{new Integer(4), xDefault};
        }
        return new Object[]{new Integer(5), arrayNode.getChild(1)};
    }

    static int lookupLanguageItem(XMPNode arrayNode, String language) throws XMPException {
        if (!arrayNode.getOptions().isArray()) {
            throw new XMPException("Language item must be used on array", 102);
        }
        for (int index = 1; index <= arrayNode.getChildrenLength(); index++) {
            XMPNode child = arrayNode.getChild(index);
            if (child.hasQualifier() && XMPConst.XML_LANG.equals(child.getQualifier(1).getName()) && language.equals(child.getQualifier(1).getValue())) {
                return index;
            }
        }
        return -1;
    }
}
