package com.adobe.xmp.impl;

import android.support.v4.app.FragmentManagerImpl;
import com.adobe.xmp.XMPDateTime;
import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.XMPUtils;
import com.adobe.xmp.impl.xpath.XMPPath;
import com.adobe.xmp.impl.xpath.XMPPathParser;
import com.adobe.xmp.options.PropertyOptions;

public class XMPMetaImpl implements XMPMeta {
    static final boolean $assertionsDisabled;
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
    public Integer getPropertyInteger(String schemaNS, String propName) throws XMPException {
        return (Integer) getPropertyObject(schemaNS, propName, 2);
    }

    @Override
    public String getPropertyString(String schemaNS, String propName) throws XMPException {
        return (String) getPropertyObject(schemaNS, propName, 0);
    }

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
    public Object clone() {
        XMPNode clonedTree = (XMPNode) this.tree.clone();
        return new XMPMetaImpl(clonedTree);
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
            case FragmentManagerImpl.ANIM_STYLE_FADE_EXIT:
                XMPDateTime dt = XMPUtils.convertToDate(rawValue);
                return dt.getCalendar();
            case 7:
                return XMPUtils.decodeBase64(rawValue);
            default:
                return (rawValue != null || propNode.getOptions().isCompositeProperty()) ? rawValue : "";
        }
    }
}
