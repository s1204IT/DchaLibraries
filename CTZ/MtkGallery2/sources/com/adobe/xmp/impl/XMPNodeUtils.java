package com.adobe.xmp.impl;

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

public class XMPNodeUtils {
    static final boolean $assertionsDisabled = false;

    private XMPNodeUtils() {
    }

    static XMPNode findSchemaNode(XMPNode xMPNode, String str, boolean z) throws XMPException {
        return findSchemaNode(xMPNode, str, null, z);
    }

    static XMPNode findSchemaNode(XMPNode xMPNode, String str, String str2, boolean z) throws XMPException {
        XMPNode xMPNodeFindChildByName = xMPNode.findChildByName(str);
        if (xMPNodeFindChildByName == null && z) {
            xMPNodeFindChildByName = new XMPNode(str, new PropertyOptions().setSchemaNode(true));
            xMPNodeFindChildByName.setImplicit(true);
            String namespacePrefix = XMPMetaFactory.getSchemaRegistry().getNamespacePrefix(str);
            if (namespacePrefix == null) {
                if (str2 != null && str2.length() != 0) {
                    namespacePrefix = XMPMetaFactory.getSchemaRegistry().registerNamespace(str, str2);
                } else {
                    throw new XMPException("Unregistered schema namespace URI", 101);
                }
            }
            xMPNodeFindChildByName.setValue(namespacePrefix);
            xMPNode.addChild(xMPNodeFindChildByName);
        }
        return xMPNodeFindChildByName;
    }

    static XMPNode findChildNode(XMPNode xMPNode, String str, boolean z) throws XMPException {
        if (!xMPNode.getOptions().isSchemaNode() && !xMPNode.getOptions().isStruct()) {
            if (!xMPNode.isImplicit()) {
                throw new XMPException("Named children only allowed for schemas and structs", 102);
            }
            if (xMPNode.getOptions().isArray()) {
                throw new XMPException("Named children not allowed for arrays", 102);
            }
            if (z) {
                xMPNode.getOptions().setStruct(true);
            }
        }
        XMPNode xMPNodeFindChildByName = xMPNode.findChildByName(str);
        if (xMPNodeFindChildByName == null && z) {
            XMPNode xMPNode2 = new XMPNode(str, new PropertyOptions());
            xMPNode2.setImplicit(true);
            xMPNode.addChild(xMPNode2);
            return xMPNode2;
        }
        return xMPNodeFindChildByName;
    }

    static XMPNode findNode(XMPNode xMPNode, XMPPath xMPPath, boolean z, PropertyOptions propertyOptions) throws XMPException {
        XMPNode xMPNode2;
        if (xMPPath == null || xMPPath.size() == 0) {
            throw new XMPException("Empty XMPPath", 102);
        }
        XMPNode xMPNodeFindSchemaNode = findSchemaNode(xMPNode, xMPPath.getSegment(0).getName(), z);
        if (xMPNodeFindSchemaNode == null) {
            return null;
        }
        if (!xMPNodeFindSchemaNode.isImplicit()) {
            xMPNode2 = null;
        } else {
            xMPNodeFindSchemaNode.setImplicit(false);
            xMPNode2 = xMPNodeFindSchemaNode;
        }
        XMPNode xMPNode3 = xMPNode2;
        XMPNode xMPNodeFollowXPathStep = xMPNodeFindSchemaNode;
        for (int i = 1; i < xMPPath.size(); i++) {
            try {
                xMPNodeFollowXPathStep = followXPathStep(xMPNodeFollowXPathStep, xMPPath.getSegment(i), z);
                if (xMPNodeFollowXPathStep == null) {
                    if (z) {
                        deleteNode(xMPNode3);
                    }
                    return null;
                }
                if (xMPNodeFollowXPathStep.isImplicit()) {
                    xMPNodeFollowXPathStep.setImplicit(false);
                    if (i == 1 && xMPPath.getSegment(i).isAlias() && xMPPath.getSegment(i).getAliasForm() != 0) {
                        xMPNodeFollowXPathStep.getOptions().setOption(xMPPath.getSegment(i).getAliasForm(), true);
                    } else if (i < xMPPath.size() - 1 && xMPPath.getSegment(i).getKind() == 1 && !xMPNodeFollowXPathStep.getOptions().isCompositeProperty()) {
                        xMPNodeFollowXPathStep.getOptions().setStruct(true);
                    }
                    if (xMPNode3 == null) {
                        xMPNode3 = xMPNodeFollowXPathStep;
                    }
                }
            } catch (XMPException e) {
                if (xMPNode3 != null) {
                    deleteNode(xMPNode3);
                }
                throw e;
            }
        }
        if (xMPNode3 != null) {
            xMPNodeFollowXPathStep.getOptions().mergeWith(propertyOptions);
            xMPNodeFollowXPathStep.setOptions(xMPNodeFollowXPathStep.getOptions());
        }
        return xMPNodeFollowXPathStep;
    }

    static void deleteNode(XMPNode xMPNode) {
        XMPNode parent = xMPNode.getParent();
        if (xMPNode.getOptions().isQualifier()) {
            parent.removeQualifier(xMPNode);
        } else {
            parent.removeChild(xMPNode);
        }
        if (!parent.hasChildren() && parent.getOptions().isSchemaNode()) {
            parent.getParent().removeChild(parent);
        }
    }

    static void setNodeValue(XMPNode xMPNode, Object obj) {
        String strSerializeNodeValue = serializeNodeValue(obj);
        if (!xMPNode.getOptions().isQualifier() || !"xml:lang".equals(xMPNode.getName())) {
            xMPNode.setValue(strSerializeNodeValue);
        } else {
            xMPNode.setValue(Utils.normalizeLangValue(strSerializeNodeValue));
        }
    }

    static PropertyOptions verifySetOptions(PropertyOptions propertyOptions, Object obj) throws XMPException {
        if (propertyOptions == null) {
            propertyOptions = new PropertyOptions();
        }
        if (propertyOptions.isArrayAltText()) {
            propertyOptions.setArrayAlternate(true);
        }
        if (propertyOptions.isArrayAlternate()) {
            propertyOptions.setArrayOrdered(true);
        }
        if (propertyOptions.isArrayOrdered()) {
            propertyOptions.setArray(true);
        }
        if (propertyOptions.isCompositeProperty() && obj != null && obj.toString().length() > 0) {
            throw new XMPException("Structs and arrays can't have values", 103);
        }
        propertyOptions.assertConsistency(propertyOptions.getOptions());
        return propertyOptions;
    }

    static String serializeNodeValue(Object obj) {
        String strEncodeBase64;
        if (obj != 0) {
            if (obj instanceof Boolean) {
                strEncodeBase64 = XMPUtils.convertFromBoolean(obj.booleanValue());
            } else if (obj instanceof Integer) {
                strEncodeBase64 = XMPUtils.convertFromInteger(obj.intValue());
            } else if (obj instanceof Long) {
                strEncodeBase64 = XMPUtils.convertFromLong(obj.longValue());
            } else if (obj instanceof Double) {
                strEncodeBase64 = XMPUtils.convertFromDouble(obj.doubleValue());
            } else if (obj instanceof XMPDateTime) {
                strEncodeBase64 = XMPUtils.convertFromDate((XMPDateTime) obj);
            } else if (obj instanceof GregorianCalendar) {
                strEncodeBase64 = XMPUtils.convertFromDate(XMPDateTimeFactory.createFromCalendar(obj));
            } else {
                strEncodeBase64 = obj instanceof byte[] ? XMPUtils.encodeBase64(obj) : obj.toString();
            }
        } else {
            strEncodeBase64 = null;
        }
        if (strEncodeBase64 != null) {
            return Utils.removeControlChars(strEncodeBase64);
        }
        return null;
    }

    private static XMPNode followXPathStep(XMPNode xMPNode, XMPPathSegment xMPPathSegment, boolean z) throws XMPException {
        int iLookupQualSelector;
        int kind = xMPPathSegment.getKind();
        if (kind == 1) {
            return findChildNode(xMPNode, xMPPathSegment.getName(), z);
        }
        if (kind == 2) {
            return findQualifierNode(xMPNode, xMPPathSegment.getName().substring(1), z);
        }
        if (!xMPNode.getOptions().isArray()) {
            throw new XMPException("Indexing applied to non-array", 102);
        }
        if (kind == 3) {
            iLookupQualSelector = findIndexedItem(xMPNode, xMPPathSegment.getName(), z);
        } else if (kind == 4) {
            iLookupQualSelector = xMPNode.getChildrenLength();
        } else if (kind == 6) {
            String[] strArrSplitNameAndValue = Utils.splitNameAndValue(xMPPathSegment.getName());
            iLookupQualSelector = lookupFieldSelector(xMPNode, strArrSplitNameAndValue[0], strArrSplitNameAndValue[1]);
        } else if (kind == 5) {
            String[] strArrSplitNameAndValue2 = Utils.splitNameAndValue(xMPPathSegment.getName());
            iLookupQualSelector = lookupQualSelector(xMPNode, strArrSplitNameAndValue2[0], strArrSplitNameAndValue2[1], xMPPathSegment.getAliasForm());
        } else {
            throw new XMPException("Unknown array indexing step in FollowXPathStep", 9);
        }
        if (1 <= iLookupQualSelector && iLookupQualSelector <= xMPNode.getChildrenLength()) {
            return xMPNode.getChild(iLookupQualSelector);
        }
        return null;
    }

    private static XMPNode findQualifierNode(XMPNode xMPNode, String str, boolean z) throws XMPException {
        XMPNode xMPNodeFindQualifierByName = xMPNode.findQualifierByName(str);
        if (xMPNodeFindQualifierByName == null && z) {
            XMPNode xMPNode2 = new XMPNode(str, null);
            xMPNode2.setImplicit(true);
            xMPNode.addQualifier(xMPNode2);
            return xMPNode2;
        }
        return xMPNodeFindQualifierByName;
    }

    private static int findIndexedItem(XMPNode xMPNode, String str, boolean z) throws XMPException {
        try {
            int i = Integer.parseInt(str.substring(1, str.length() - 1));
            if (i < 1) {
                throw new XMPException("Array index must be larger than zero", 102);
            }
            if (z && i == xMPNode.getChildrenLength() + 1) {
                XMPNode xMPNode2 = new XMPNode("[]", null);
                xMPNode2.setImplicit(true);
                xMPNode.addChild(xMPNode2);
            }
            return i;
        } catch (NumberFormatException e) {
            throw new XMPException("Array index not digits.", 102);
        }
    }

    private static int lookupFieldSelector(XMPNode xMPNode, String str, String str2) throws XMPException {
        int i = -1;
        for (int i2 = 1; i2 <= xMPNode.getChildrenLength() && i < 0; i2++) {
            XMPNode child = xMPNode.getChild(i2);
            if (!child.getOptions().isStruct()) {
                throw new XMPException("Field selector must be used on array of struct", 102);
            }
            int i3 = 1;
            while (true) {
                if (i3 <= child.getChildrenLength()) {
                    XMPNode child2 = child.getChild(i3);
                    if (str.equals(child2.getName()) && str2.equals(child2.getValue())) {
                        i = i2;
                        break;
                    }
                    i3++;
                }
            }
        }
        return i;
    }

    private static int lookupQualSelector(XMPNode xMPNode, String str, String str2, int i) throws XMPException {
        if ("xml:lang".equals(str)) {
            int iLookupLanguageItem = lookupLanguageItem(xMPNode, Utils.normalizeLangValue(str2));
            if (iLookupLanguageItem < 0 && (i & 4096) > 0) {
                XMPNode xMPNode2 = new XMPNode("[]", null);
                xMPNode2.addQualifier(new XMPNode("xml:lang", "x-default", null));
                xMPNode.addChild(1, xMPNode2);
                return 1;
            }
            return iLookupLanguageItem;
        }
        for (int i2 = 1; i2 < xMPNode.getChildrenLength(); i2++) {
            Iterator itIterateQualifier = xMPNode.getChild(i2).iterateQualifier();
            while (itIterateQualifier.hasNext()) {
                XMPNode xMPNode3 = (XMPNode) itIterateQualifier.next();
                if (str.equals(xMPNode3.getName()) && str2.equals(xMPNode3.getValue())) {
                    return i2;
                }
            }
        }
        return -1;
    }

    static int lookupLanguageItem(XMPNode xMPNode, String str) throws XMPException {
        if (!xMPNode.getOptions().isArray()) {
            throw new XMPException("Language item must be used on array", 102);
        }
        for (int i = 1; i <= xMPNode.getChildrenLength(); i++) {
            XMPNode child = xMPNode.getChild(i);
            if (child.hasQualifier() && "xml:lang".equals(child.getQualifier(1).getName()) && str.equals(child.getQualifier(1).getValue())) {
                return i;
            }
        }
        return -1;
    }
}
