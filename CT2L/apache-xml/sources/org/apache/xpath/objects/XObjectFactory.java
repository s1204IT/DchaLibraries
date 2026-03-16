package org.apache.xpath.objects;

import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMAxisIterator;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xml.utils.WrappedRuntimeException;
import org.apache.xpath.XPathContext;
import org.apache.xpath.axes.OneStepIterator;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.NodeIterator;

public class XObjectFactory {
    public static XObject create(Object val) {
        if (val instanceof XObject) {
            XObject result = (XObject) val;
            return result;
        }
        if (val instanceof String) {
            XObject result2 = new XString((String) val);
            return result2;
        }
        if (val instanceof Boolean) {
            XObject result3 = new XBoolean((Boolean) val);
            return result3;
        }
        if (val instanceof Double) {
            XObject result4 = new XNumber((Double) val);
            return result4;
        }
        XObject result5 = new XObject(val);
        return result5;
    }

    public static XObject create(Object val, XPathContext xctxt) {
        if (val instanceof XObject) {
            XObject result = (XObject) val;
            return result;
        }
        if (val instanceof String) {
            XObject result2 = new XString((String) val);
            return result2;
        }
        if (val instanceof Boolean) {
            XObject result3 = new XBoolean((Boolean) val);
            return result3;
        }
        if (val instanceof Number) {
            XObject result4 = new XNumber((Number) val);
            return result4;
        }
        if (val instanceof DTM) {
            DTM dtm = (DTM) val;
            try {
                int dtmRoot = dtm.getDocument();
                DTMAxisIterator iter = dtm.getAxisIterator(13);
                iter.setStartNode(dtmRoot);
                DTMIterator iterator = new OneStepIterator(iter, 13);
                iterator.setRoot(dtmRoot, xctxt);
                XObject result5 = new XNodeSet(iterator);
                return result5;
            } catch (Exception ex) {
                throw new WrappedRuntimeException(ex);
            }
        }
        if (val instanceof DTMAxisIterator) {
            DTMAxisIterator iter2 = (DTMAxisIterator) val;
            try {
                DTMIterator iterator2 = new OneStepIterator(iter2, 13);
                iterator2.setRoot(iter2.getStartNode(), xctxt);
                XObject result6 = new XNodeSet(iterator2);
                return result6;
            } catch (Exception ex2) {
                throw new WrappedRuntimeException(ex2);
            }
        }
        if (val instanceof DTMIterator) {
            XObject result7 = new XNodeSet((DTMIterator) val);
            return result7;
        }
        if (val instanceof Node) {
            XObject result8 = new XNodeSetForDOM((Node) val, xctxt);
            return result8;
        }
        if (val instanceof NodeList) {
            XObject result9 = new XNodeSetForDOM((NodeList) val, xctxt);
            return result9;
        }
        if (val instanceof NodeIterator) {
            XObject result10 = new XNodeSetForDOM((NodeIterator) val, xctxt);
            return result10;
        }
        XObject result11 = new XObject(val);
        return result11;
    }
}
