package com.adobe.xmp.impl;

import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.XMPUtils;
import com.adobe.xmp.impl.xpath.XMPPathParser;
import com.adobe.xmp.options.PropertyOptions;

public class XMPMetaImpl implements XMPMeta {
    static final boolean $assertionsDisabled = false;
    private String packetHeader;
    private XMPNode tree;

    public XMPMetaImpl() {
        this.packetHeader = null;
        this.tree = new XMPNode(null, null, null);
    }

    public XMPMetaImpl(XMPNode xMPNode) {
        this.packetHeader = null;
        this.tree = xMPNode;
    }

    @Override
    public boolean doesPropertyExist(String str, String str2) {
        try {
            ParameterAsserts.assertSchemaNS(str);
            ParameterAsserts.assertPropName(str2);
            return XMPNodeUtils.findNode(this.tree, XMPPathParser.expandXPath(str, str2), false, null) != null;
        } catch (XMPException e) {
            return false;
        }
    }

    protected Object getPropertyObject(String str, String str2, int i) throws XMPException {
        ParameterAsserts.assertSchemaNS(str);
        ParameterAsserts.assertPropName(str2);
        XMPNode xMPNodeFindNode = XMPNodeUtils.findNode(this.tree, XMPPathParser.expandXPath(str, str2), false, null);
        if (xMPNodeFindNode == null) {
            return null;
        }
        if (i != 0 && xMPNodeFindNode.getOptions().isCompositeProperty()) {
            throw new XMPException("Property must be simple when a value type is requested", 102);
        }
        return evaluateNodeValue(i, xMPNodeFindNode);
    }

    @Override
    public Integer getPropertyInteger(String str, String str2) throws XMPException {
        return (Integer) getPropertyObject(str, str2, 2);
    }

    @Override
    public String getPropertyString(String str, String str2) throws XMPException {
        return (String) getPropertyObject(str, str2, 0);
    }

    public void setProperty(String str, String str2, Object obj, PropertyOptions propertyOptions) throws XMPException {
        ParameterAsserts.assertSchemaNS(str);
        ParameterAsserts.assertPropName(str2);
        PropertyOptions propertyOptionsVerifySetOptions = XMPNodeUtils.verifySetOptions(propertyOptions, obj);
        XMPNode xMPNodeFindNode = XMPNodeUtils.findNode(this.tree, XMPPathParser.expandXPath(str, str2), true, propertyOptionsVerifySetOptions);
        if (xMPNodeFindNode != null) {
            setNode(xMPNodeFindNode, obj, propertyOptionsVerifySetOptions, false);
            return;
        }
        throw new XMPException("Specified property does not exist", 102);
    }

    @Override
    public void setProperty(String str, String str2, Object obj) throws XMPException {
        setProperty(str, str2, obj, null);
    }

    @Override
    public Object clone() {
        return new XMPMetaImpl((XMPNode) this.tree.clone());
    }

    void setNode(XMPNode xMPNode, Object obj, PropertyOptions propertyOptions, boolean z) throws XMPException {
        if (z) {
            xMPNode.clear();
        }
        xMPNode.getOptions().mergeWith(propertyOptions);
        if (!xMPNode.getOptions().isCompositeProperty()) {
            XMPNodeUtils.setNodeValue(xMPNode, obj);
        } else {
            if (obj != null && obj.toString().length() > 0) {
                throw new XMPException("Composite nodes can't have values", 102);
            }
            xMPNode.removeChildren();
        }
    }

    private Object evaluateNodeValue(int i, XMPNode xMPNode) throws XMPException {
        String value = xMPNode.getValue();
        switch (i) {
            case 1:
                return new Boolean(XMPUtils.convertToBoolean(value));
            case 2:
                return new Integer(XMPUtils.convertToInteger(value));
            case 3:
                return new Long(XMPUtils.convertToLong(value));
            case 4:
                return new Double(XMPUtils.convertToDouble(value));
            case 5:
                return XMPUtils.convertToDate(value);
            case 6:
                return XMPUtils.convertToDate(value).getCalendar();
            case 7:
                return XMPUtils.decodeBase64(value);
            default:
                if (value == null && !xMPNode.getOptions().isCompositeProperty()) {
                    value = "";
                }
                return value;
        }
    }
}
