package com.adobe.xmp.impl;

import com.adobe.xmp.XMPConst;
import com.adobe.xmp.XMPDateTime;
import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPIterator;
import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.XMPPathFactory;
import com.adobe.xmp.XMPUtils;
import com.adobe.xmp.impl.xpath.XMPPath;
import com.adobe.xmp.impl.xpath.XMPPathParser;
import com.adobe.xmp.options.IteratorOptions;
import com.adobe.xmp.options.ParseOptions;
import com.adobe.xmp.options.PropertyOptions;
import com.adobe.xmp.properties.XMPProperty;
import java.util.Calendar;
import java.util.Iterator;

public class XMPMetaImpl implements XMPMeta, XMPConst {
    static final boolean $assertionsDisabled;
    private static final int VALUE_BASE64 = 7;
    private static final int VALUE_BOOLEAN = 1;
    private static final int VALUE_CALENDAR = 6;
    private static final int VALUE_DATE = 5;
    private static final int VALUE_DOUBLE = 4;
    private static final int VALUE_INTEGER = 2;
    private static final int VALUE_LONG = 3;
    private static final int VALUE_STRING = 0;
    private String packetHeader;
    private XMPNode tree;

    static {
        $assertionsDisabled = !XMPMetaImpl.class.desiredAssertionStatus();
    }

    public XMPMetaImpl() {
        this.packetHeader = null;
        this.tree = new XMPNode(null, null, null);
    }

    public XMPMetaImpl(XMPNode tree) {
        this.packetHeader = null;
        this.tree = tree;
    }

    @Override
    public void appendArrayItem(String schemaNS, String arrayName, PropertyOptions arrayOptions, String itemValue, PropertyOptions itemOptions) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertArrayName(arrayName);
        if (arrayOptions == null) {
            arrayOptions = new PropertyOptions();
        }
        if (!arrayOptions.isOnlyArrayOptions()) {
            throw new XMPException("Only array form flags allowed for arrayOptions", 103);
        }
        PropertyOptions arrayOptions2 = XMPNodeUtils.verifySetOptions(arrayOptions, null);
        XMPPath arrayPath = XMPPathParser.expandXPath(schemaNS, arrayName);
        XMPNode arrayNode = XMPNodeUtils.findNode(this.tree, arrayPath, false, null);
        if (arrayNode != null) {
            if (!arrayNode.getOptions().isArray()) {
                throw new XMPException("The named property is not an array", 102);
            }
        } else if (arrayOptions2.isArray()) {
            arrayNode = XMPNodeUtils.findNode(this.tree, arrayPath, true, arrayOptions2);
            if (arrayNode == null) {
                throw new XMPException("Failure creating array node", 102);
            }
        } else {
            throw new XMPException("Explicit arrayOptions required to create new array", 103);
        }
        doSetArrayItem(arrayNode, -1, itemValue, itemOptions, true);
    }

    @Override
    public void appendArrayItem(String schemaNS, String arrayName, String itemValue) throws XMPException {
        appendArrayItem(schemaNS, arrayName, null, itemValue, null);
    }

    @Override
    public int countArrayItems(String schemaNS, String arrayName) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertArrayName(arrayName);
        XMPPath arrayPath = XMPPathParser.expandXPath(schemaNS, arrayName);
        XMPNode arrayNode = XMPNodeUtils.findNode(this.tree, arrayPath, false, null);
        if (arrayNode == null) {
            return 0;
        }
        if (arrayNode.getOptions().isArray()) {
            return arrayNode.getChildrenLength();
        }
        throw new XMPException("The named property is not an array", 102);
    }

    @Override
    public void deleteArrayItem(String schemaNS, String arrayName, int itemIndex) {
        try {
            ParameterAsserts.assertSchemaNS(schemaNS);
            ParameterAsserts.assertArrayName(arrayName);
            String itemPath = XMPPathFactory.composeArrayItemPath(arrayName, itemIndex);
            deleteProperty(schemaNS, itemPath);
        } catch (XMPException e) {
        }
    }

    @Override
    public void deleteProperty(String schemaNS, String propName) {
        try {
            ParameterAsserts.assertSchemaNS(schemaNS);
            ParameterAsserts.assertPropName(propName);
            XMPPath expPath = XMPPathParser.expandXPath(schemaNS, propName);
            XMPNode propNode = XMPNodeUtils.findNode(this.tree, expPath, false, null);
            if (propNode != null) {
                XMPNodeUtils.deleteNode(propNode);
            }
        } catch (XMPException e) {
        }
    }

    @Override
    public void deleteQualifier(String schemaNS, String propName, String qualNS, String qualName) {
        try {
            ParameterAsserts.assertSchemaNS(schemaNS);
            ParameterAsserts.assertPropName(propName);
            String qualPath = propName + XMPPathFactory.composeQualifierPath(qualNS, qualName);
            deleteProperty(schemaNS, qualPath);
        } catch (XMPException e) {
        }
    }

    @Override
    public void deleteStructField(String schemaNS, String structName, String fieldNS, String fieldName) {
        try {
            ParameterAsserts.assertSchemaNS(schemaNS);
            ParameterAsserts.assertStructName(structName);
            String fieldPath = structName + XMPPathFactory.composeStructFieldPath(fieldNS, fieldName);
            deleteProperty(schemaNS, fieldPath);
        } catch (XMPException e) {
        }
    }

    @Override
    public boolean doesPropertyExist(String schemaNS, String propName) {
        try {
            ParameterAsserts.assertSchemaNS(schemaNS);
            ParameterAsserts.assertPropName(propName);
            XMPPath expPath = XMPPathParser.expandXPath(schemaNS, propName);
            XMPNode propNode = XMPNodeUtils.findNode(this.tree, expPath, false, null);
            return propNode != null;
        } catch (XMPException e) {
            return false;
        }
    }

    @Override
    public boolean doesArrayItemExist(String schemaNS, String arrayName, int itemIndex) {
        try {
            ParameterAsserts.assertSchemaNS(schemaNS);
            ParameterAsserts.assertArrayName(arrayName);
            String path = XMPPathFactory.composeArrayItemPath(arrayName, itemIndex);
            return doesPropertyExist(schemaNS, path);
        } catch (XMPException e) {
            return false;
        }
    }

    @Override
    public boolean doesStructFieldExist(String schemaNS, String structName, String fieldNS, String fieldName) {
        try {
            ParameterAsserts.assertSchemaNS(schemaNS);
            ParameterAsserts.assertStructName(structName);
            String path = XMPPathFactory.composeStructFieldPath(fieldNS, fieldName);
            return doesPropertyExist(schemaNS, structName + path);
        } catch (XMPException e) {
            return false;
        }
    }

    @Override
    public boolean doesQualifierExist(String schemaNS, String propName, String qualNS, String qualName) {
        try {
            ParameterAsserts.assertSchemaNS(schemaNS);
            ParameterAsserts.assertPropName(propName);
            String path = XMPPathFactory.composeQualifierPath(qualNS, qualName);
            return doesPropertyExist(schemaNS, propName + path);
        } catch (XMPException e) {
            return false;
        }
    }

    @Override
    public XMPProperty getArrayItem(String schemaNS, String arrayName, int itemIndex) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertArrayName(arrayName);
        String itemPath = XMPPathFactory.composeArrayItemPath(arrayName, itemIndex);
        return getProperty(schemaNS, itemPath);
    }

    @Override
    public XMPProperty getLocalizedText(String schemaNS, String altTextName, String genericLang, String specificLang) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertArrayName(altTextName);
        ParameterAsserts.assertSpecificLang(specificLang);
        String genericLang2 = genericLang != null ? Utils.normalizeLangValue(genericLang) : null;
        String specificLang2 = Utils.normalizeLangValue(specificLang);
        XMPPath arrayPath = XMPPathParser.expandXPath(schemaNS, altTextName);
        XMPNode arrayNode = XMPNodeUtils.findNode(this.tree, arrayPath, false, null);
        if (arrayNode == null) {
            return null;
        }
        Object[] result = XMPNodeUtils.chooseLocalizedText(arrayNode, genericLang2, specificLang2);
        int match = ((Integer) result[0]).intValue();
        final XMPNode itemNode = (XMPNode) result[1];
        if (match != 0) {
            return new XMPProperty() {
                @Override
                public Object getValue() {
                    return itemNode.getValue();
                }

                @Override
                public PropertyOptions getOptions() {
                    return itemNode.getOptions();
                }

                @Override
                public String getLanguage() {
                    return itemNode.getQualifier(1).getValue();
                }

                public String toString() {
                    return itemNode.getValue().toString();
                }
            };
        }
        return null;
    }

    @Override
    public void setLocalizedText(String schemaNS, String altTextName, String genericLang, String specificLang, String itemValue, PropertyOptions options) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertArrayName(altTextName);
        ParameterAsserts.assertSpecificLang(specificLang);
        String genericLang2 = genericLang != null ? Utils.normalizeLangValue(genericLang) : null;
        String specificLang2 = Utils.normalizeLangValue(specificLang);
        XMPPath arrayPath = XMPPathParser.expandXPath(schemaNS, altTextName);
        XMPNode arrayNode = XMPNodeUtils.findNode(this.tree, arrayPath, true, new PropertyOptions(7680));
        if (arrayNode == null) {
            throw new XMPException("Failed to find or create array node", 102);
        }
        if (!arrayNode.getOptions().isArrayAltText()) {
            if (!arrayNode.hasChildren() && arrayNode.getOptions().isArrayAlternate()) {
                arrayNode.getOptions().setArrayAltText(true);
            } else {
                throw new XMPException("Specified property is no alt-text array", 102);
            }
        }
        boolean haveXDefault = false;
        XMPNode xdItem = null;
        Iterator it = arrayNode.iterateChildren();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            XMPNode currItem = (XMPNode) it.next();
            if (!currItem.hasQualifier() || !XMPConst.XML_LANG.equals(currItem.getQualifier(1).getName())) {
                break;
            }
            if (XMPConst.X_DEFAULT.equals(currItem.getQualifier(1).getValue())) {
                xdItem = currItem;
                haveXDefault = true;
                break;
            }
        }
        if (xdItem != null && arrayNode.getChildrenLength() > 1) {
            arrayNode.removeChild(xdItem);
            arrayNode.addChild(1, xdItem);
        }
        Object[] result = XMPNodeUtils.chooseLocalizedText(arrayNode, genericLang2, specificLang2);
        int match = ((Integer) result[0]).intValue();
        XMPNode itemNode = (XMPNode) result[1];
        boolean specificXDefault = XMPConst.X_DEFAULT.equals(specificLang2);
        switch (match) {
            case 0:
                XMPNodeUtils.appendLangItem(arrayNode, XMPConst.X_DEFAULT, itemValue);
                haveXDefault = true;
                if (!specificXDefault) {
                    XMPNodeUtils.appendLangItem(arrayNode, specificLang2, itemValue);
                }
                break;
            case 1:
                if (!specificXDefault) {
                    if (haveXDefault && xdItem != itemNode && xdItem != null && xdItem.getValue().equals(itemNode.getValue())) {
                        xdItem.setValue(itemValue);
                    }
                    itemNode.setValue(itemValue);
                } else {
                    if (!$assertionsDisabled && (!haveXDefault || xdItem != itemNode)) {
                        throw new AssertionError();
                    }
                    Iterator it2 = arrayNode.iterateChildren();
                    while (it2.hasNext()) {
                        XMPNode currItem2 = (XMPNode) it2.next();
                        if (currItem2 != xdItem) {
                            if (currItem2.getValue().equals(xdItem != null ? xdItem.getValue() : null)) {
                                currItem2.setValue(itemValue);
                            }
                        }
                    }
                    if (xdItem != null) {
                        xdItem.setValue(itemValue);
                    }
                }
                break;
            case 2:
                if (haveXDefault && xdItem != itemNode && xdItem != null && xdItem.getValue().equals(itemNode.getValue())) {
                    xdItem.setValue(itemValue);
                }
                itemNode.setValue(itemValue);
                break;
            case 3:
                XMPNodeUtils.appendLangItem(arrayNode, specificLang2, itemValue);
                if (specificXDefault) {
                    haveXDefault = true;
                }
                break;
            case 4:
                if (xdItem != null && arrayNode.getChildrenLength() == 1) {
                    xdItem.setValue(itemValue);
                }
                XMPNodeUtils.appendLangItem(arrayNode, specificLang2, itemValue);
                break;
            case 5:
                XMPNodeUtils.appendLangItem(arrayNode, specificLang2, itemValue);
                if (specificXDefault) {
                    haveXDefault = true;
                }
                break;
            default:
                throw new XMPException("Unexpected result from ChooseLocalizedText", 9);
        }
        if (!haveXDefault && arrayNode.getChildrenLength() == 1) {
            XMPNodeUtils.appendLangItem(arrayNode, XMPConst.X_DEFAULT, itemValue);
        }
    }

    @Override
    public void setLocalizedText(String schemaNS, String altTextName, String genericLang, String specificLang, String itemValue) throws XMPException {
        setLocalizedText(schemaNS, altTextName, genericLang, specificLang, itemValue, null);
    }

    @Override
    public XMPProperty getProperty(String schemaNS, String propName) throws XMPException {
        return getProperty(schemaNS, propName, 0);
    }

    protected XMPProperty getProperty(String schemaNS, String propName, int valueType) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertPropName(propName);
        XMPPath expPath = XMPPathParser.expandXPath(schemaNS, propName);
        final XMPNode propNode = XMPNodeUtils.findNode(this.tree, expPath, false, null);
        if (propNode == null) {
            return null;
        }
        if (valueType != 0 && propNode.getOptions().isCompositeProperty()) {
            throw new XMPException("Property must be simple when a value type is requested", 102);
        }
        final Object value = evaluateNodeValue(valueType, propNode);
        return new XMPProperty() {
            @Override
            public Object getValue() {
                return value;
            }

            @Override
            public PropertyOptions getOptions() {
                return propNode.getOptions();
            }

            @Override
            public String getLanguage() {
                return null;
            }

            public String toString() {
                return value.toString();
            }
        };
    }

    protected Object getPropertyObject(String schemaNS, String propName, int valueType) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertPropName(propName);
        XMPPath expPath = XMPPathParser.expandXPath(schemaNS, propName);
        XMPNode propNode = XMPNodeUtils.findNode(this.tree, expPath, false, null);
        if (propNode == null) {
            return null;
        }
        if (valueType != 0 && propNode.getOptions().isCompositeProperty()) {
            throw new XMPException("Property must be simple when a value type is requested", 102);
        }
        return evaluateNodeValue(valueType, propNode);
    }

    @Override
    public Boolean getPropertyBoolean(String schemaNS, String propName) throws XMPException {
        return (Boolean) getPropertyObject(schemaNS, propName, 1);
    }

    @Override
    public void setPropertyBoolean(String schemaNS, String propName, boolean propValue, PropertyOptions options) throws XMPException {
        setProperty(schemaNS, propName, propValue ? XMPConst.TRUESTR : XMPConst.FALSESTR, options);
    }

    @Override
    public void setPropertyBoolean(String schemaNS, String propName, boolean propValue) throws XMPException {
        setProperty(schemaNS, propName, propValue ? XMPConst.TRUESTR : XMPConst.FALSESTR, null);
    }

    @Override
    public Integer getPropertyInteger(String schemaNS, String propName) throws XMPException {
        return (Integer) getPropertyObject(schemaNS, propName, 2);
    }

    @Override
    public void setPropertyInteger(String schemaNS, String propName, int propValue, PropertyOptions options) throws XMPException {
        setProperty(schemaNS, propName, new Integer(propValue), options);
    }

    @Override
    public void setPropertyInteger(String schemaNS, String propName, int propValue) throws XMPException {
        setProperty(schemaNS, propName, new Integer(propValue), null);
    }

    @Override
    public Long getPropertyLong(String schemaNS, String propName) throws XMPException {
        return (Long) getPropertyObject(schemaNS, propName, 3);
    }

    @Override
    public void setPropertyLong(String schemaNS, String propName, long propValue, PropertyOptions options) throws XMPException {
        setProperty(schemaNS, propName, new Long(propValue), options);
    }

    @Override
    public void setPropertyLong(String schemaNS, String propName, long propValue) throws XMPException {
        setProperty(schemaNS, propName, new Long(propValue), null);
    }

    @Override
    public Double getPropertyDouble(String schemaNS, String propName) throws XMPException {
        return (Double) getPropertyObject(schemaNS, propName, 4);
    }

    @Override
    public void setPropertyDouble(String schemaNS, String propName, double propValue, PropertyOptions options) throws XMPException {
        setProperty(schemaNS, propName, new Double(propValue), options);
    }

    @Override
    public void setPropertyDouble(String schemaNS, String propName, double propValue) throws XMPException {
        setProperty(schemaNS, propName, new Double(propValue), null);
    }

    @Override
    public XMPDateTime getPropertyDate(String schemaNS, String propName) throws XMPException {
        return (XMPDateTime) getPropertyObject(schemaNS, propName, 5);
    }

    @Override
    public void setPropertyDate(String schemaNS, String propName, XMPDateTime propValue, PropertyOptions options) throws XMPException {
        setProperty(schemaNS, propName, propValue, options);
    }

    @Override
    public void setPropertyDate(String schemaNS, String propName, XMPDateTime propValue) throws XMPException {
        setProperty(schemaNS, propName, propValue, null);
    }

    @Override
    public Calendar getPropertyCalendar(String schemaNS, String propName) throws XMPException {
        return (Calendar) getPropertyObject(schemaNS, propName, 6);
    }

    @Override
    public void setPropertyCalendar(String schemaNS, String propName, Calendar propValue, PropertyOptions options) throws XMPException {
        setProperty(schemaNS, propName, propValue, options);
    }

    @Override
    public void setPropertyCalendar(String schemaNS, String propName, Calendar propValue) throws XMPException {
        setProperty(schemaNS, propName, propValue, null);
    }

    @Override
    public byte[] getPropertyBase64(String schemaNS, String propName) throws XMPException {
        return (byte[]) getPropertyObject(schemaNS, propName, 7);
    }

    @Override
    public String getPropertyString(String schemaNS, String propName) throws XMPException {
        return (String) getPropertyObject(schemaNS, propName, 0);
    }

    @Override
    public void setPropertyBase64(String schemaNS, String propName, byte[] propValue, PropertyOptions options) throws XMPException {
        setProperty(schemaNS, propName, propValue, options);
    }

    @Override
    public void setPropertyBase64(String schemaNS, String propName, byte[] propValue) throws XMPException {
        setProperty(schemaNS, propName, propValue, null);
    }

    @Override
    public XMPProperty getQualifier(String schemaNS, String propName, String qualNS, String qualName) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertPropName(propName);
        String qualPath = propName + XMPPathFactory.composeQualifierPath(qualNS, qualName);
        return getProperty(schemaNS, qualPath);
    }

    @Override
    public XMPProperty getStructField(String schemaNS, String structName, String fieldNS, String fieldName) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertStructName(structName);
        String fieldPath = structName + XMPPathFactory.composeStructFieldPath(fieldNS, fieldName);
        return getProperty(schemaNS, fieldPath);
    }

    @Override
    public XMPIterator iterator() throws XMPException {
        return iterator(null, null, null);
    }

    @Override
    public XMPIterator iterator(IteratorOptions options) throws XMPException {
        return iterator(null, null, options);
    }

    @Override
    public XMPIterator iterator(String schemaNS, String propName, IteratorOptions options) throws XMPException {
        return new XMPIteratorImpl(this, schemaNS, propName, options);
    }

    @Override
    public void setArrayItem(String schemaNS, String arrayName, int itemIndex, String itemValue, PropertyOptions options) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertArrayName(arrayName);
        XMPPath arrayPath = XMPPathParser.expandXPath(schemaNS, arrayName);
        XMPNode arrayNode = XMPNodeUtils.findNode(this.tree, arrayPath, false, null);
        if (arrayNode != null) {
            doSetArrayItem(arrayNode, itemIndex, itemValue, options, false);
            return;
        }
        throw new XMPException("Specified array does not exist", 102);
    }

    @Override
    public void setArrayItem(String schemaNS, String arrayName, int itemIndex, String itemValue) throws XMPException {
        setArrayItem(schemaNS, arrayName, itemIndex, itemValue, null);
    }

    @Override
    public void insertArrayItem(String schemaNS, String arrayName, int itemIndex, String itemValue, PropertyOptions options) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertArrayName(arrayName);
        XMPPath arrayPath = XMPPathParser.expandXPath(schemaNS, arrayName);
        XMPNode arrayNode = XMPNodeUtils.findNode(this.tree, arrayPath, false, null);
        if (arrayNode != null) {
            doSetArrayItem(arrayNode, itemIndex, itemValue, options, true);
            return;
        }
        throw new XMPException("Specified array does not exist", 102);
    }

    @Override
    public void insertArrayItem(String schemaNS, String arrayName, int itemIndex, String itemValue) throws XMPException {
        insertArrayItem(schemaNS, arrayName, itemIndex, itemValue, null);
    }

    @Override
    public void setProperty(String schemaNS, String propName, Object propValue, PropertyOptions options) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertPropName(propName);
        PropertyOptions options2 = XMPNodeUtils.verifySetOptions(options, propValue);
        XMPPath expPath = XMPPathParser.expandXPath(schemaNS, propName);
        XMPNode propNode = XMPNodeUtils.findNode(this.tree, expPath, true, options2);
        if (propNode != null) {
            setNode(propNode, propValue, options2, false);
            return;
        }
        throw new XMPException("Specified property does not exist", 102);
    }

    @Override
    public void setProperty(String schemaNS, String propName, Object propValue) throws XMPException {
        setProperty(schemaNS, propName, propValue, null);
    }

    @Override
    public void setQualifier(String schemaNS, String propName, String qualNS, String qualName, String qualValue, PropertyOptions options) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertPropName(propName);
        if (!doesPropertyExist(schemaNS, propName)) {
            throw new XMPException("Specified property does not exist!", 102);
        }
        String qualPath = propName + XMPPathFactory.composeQualifierPath(qualNS, qualName);
        setProperty(schemaNS, qualPath, qualValue, options);
    }

    @Override
    public void setQualifier(String schemaNS, String propName, String qualNS, String qualName, String qualValue) throws XMPException {
        setQualifier(schemaNS, propName, qualNS, qualName, qualValue, null);
    }

    @Override
    public void setStructField(String schemaNS, String structName, String fieldNS, String fieldName, String fieldValue, PropertyOptions options) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertStructName(structName);
        String fieldPath = structName + XMPPathFactory.composeStructFieldPath(fieldNS, fieldName);
        setProperty(schemaNS, fieldPath, fieldValue, options);
    }

    @Override
    public void setStructField(String schemaNS, String structName, String fieldNS, String fieldName, String fieldValue) throws XMPException {
        setStructField(schemaNS, structName, fieldNS, fieldName, fieldValue, null);
    }

    @Override
    public String getObjectName() {
        return this.tree.getName() != null ? this.tree.getName() : "";
    }

    @Override
    public void setObjectName(String name) {
        this.tree.setName(name);
    }

    @Override
    public String getPacketHeader() {
        return this.packetHeader;
    }

    public void setPacketHeader(String packetHeader) {
        this.packetHeader = packetHeader;
    }

    @Override
    public Object clone() {
        XMPNode clonedTree = (XMPNode) this.tree.clone();
        return new XMPMetaImpl(clonedTree);
    }

    @Override
    public String dumpObject() {
        return getRoot().dumpNode(true);
    }

    @Override
    public void sort() {
        this.tree.sort();
    }

    @Override
    public void normalize(ParseOptions options) throws XMPException {
        if (options == null) {
            options = new ParseOptions();
        }
        XMPNormalizer.process(this, options);
    }

    public XMPNode getRoot() {
        return this.tree;
    }

    private void doSetArrayItem(XMPNode arrayNode, int itemIndex, String itemValue, PropertyOptions itemOptions, boolean insert) throws XMPException {
        XMPNode itemNode = new XMPNode(XMPConst.ARRAY_ITEM_NAME, null);
        PropertyOptions itemOptions2 = XMPNodeUtils.verifySetOptions(itemOptions, itemValue);
        int maxIndex = insert ? arrayNode.getChildrenLength() + 1 : arrayNode.getChildrenLength();
        if (itemIndex == -1) {
            itemIndex = maxIndex;
        }
        if (1 <= itemIndex && itemIndex <= maxIndex) {
            if (!insert) {
                arrayNode.removeChild(itemIndex);
            }
            arrayNode.addChild(itemIndex, itemNode);
            setNode(itemNode, itemValue, itemOptions2, false);
            return;
        }
        throw new XMPException("Array index out of bounds", 104);
    }

    void setNode(XMPNode node, Object value, PropertyOptions newOptions, boolean deleteExisting) throws XMPException {
        if (deleteExisting) {
            node.clear();
        }
        node.getOptions().mergeWith(newOptions);
        if (!node.getOptions().isCompositeProperty()) {
            XMPNodeUtils.setNodeValue(node, value);
        } else {
            if (value != null && value.toString().length() > 0) {
                throw new XMPException("Composite nodes can't have values", 102);
            }
            node.removeChildren();
        }
    }

    private Object evaluateNodeValue(int valueType, XMPNode propNode) throws XMPException {
        String rawValue = propNode.getValue();
        switch (valueType) {
            case 1:
                return new Boolean(XMPUtils.convertToBoolean(rawValue));
            case 2:
                return new Integer(XMPUtils.convertToInteger(rawValue));
            case 3:
                return new Long(XMPUtils.convertToLong(rawValue));
            case 4:
                return new Double(XMPUtils.convertToDouble(rawValue));
            case 5:
                return XMPUtils.convertToDate(rawValue);
            case 6:
                XMPDateTime dt = XMPUtils.convertToDate(rawValue);
                return dt.getCalendar();
            case 7:
                return XMPUtils.decodeBase64(rawValue);
            default:
                return (rawValue != null || propNode.getOptions().isCompositeProperty()) ? rawValue : "";
        }
    }
}
