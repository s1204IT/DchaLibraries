package com.adobe.xmp.impl;

import com.adobe.xmp.XMPException;

class ParameterAsserts {
    public static void assertPropName(String propName) throws XMPException {
        if (propName == null || propName.length() == 0) {
            throw new XMPException("Empty property name", 4);
        }
    }

    public static void assertSchemaNS(String schemaNS) throws XMPException {
        if (schemaNS == null || schemaNS.length() == 0) {
            throw new XMPException("Empty schema namespace URI", 4);
        }
    }

    public static void assertPrefix(String prefix) throws XMPException {
        if (prefix == null || prefix.length() == 0) {
            throw new XMPException("Empty prefix", 4);
        }
    }

    public static void assertNotNull(Object param) throws XMPException {
        if (param == null) {
            throw new XMPException("Parameter must not be null", 4);
        }
        if ((param instanceof String) && ((String) param).length() == 0) {
            throw new XMPException("Parameter must not be null or empty", 4);
        }
    }
}
