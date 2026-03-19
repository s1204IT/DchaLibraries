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
    public static XObject create(Object obj) {
        if (obj instanceof XObject) {
            return obj;
        }
        if (obj instanceof String) {
            XObject result = new XString((String) obj);
            return result;
        }
        if (obj instanceof Boolean) {
            XObject result2 = new XBoolean((Boolean) obj);
            return result2;
        }
        if (obj instanceof Double) {
            XObject result3 = new XNumber((Number) obj);
            return result3;
        }
        XObject result4 = new XObject(obj);
        return result4;
    }

    public static XObject create(Object obj, XPathContext xctxt) {
        if (obj instanceof XObject) {
            return obj;
        }
        if (obj instanceof String) {
            XObject result = new XString((String) obj);
            return result;
        }
        if (obj instanceof Boolean) {
            XObject result2 = new XBoolean((Boolean) obj);
            return result2;
        }
        if (obj instanceof Number) {
            XObject result3 = new XNumber((Number) obj);
            return result3;
        }
        if (obj instanceof DTM) {
            DTM dtm = (DTM) obj;
            try {
                int dtmRoot = dtm.getDocument();
                DTMAxisIterator iter = dtm.getAxisIterator(13);
                iter.setStartNode(dtmRoot);
                DTMIterator iterator = new OneStepIterator(iter, 13);
                iterator.setRoot(dtmRoot, xctxt);
                XObject result4 = new XNodeSet(iterator);
                return result4;
            } catch (Exception ex) {
                throw new WrappedRuntimeException(ex);
            }
        }
        if (obj instanceof DTMAxisIterator) {
            DTMAxisIterator iter2 = (DTMAxisIterator) obj;
            try {
                DTMIterator iterator2 = new OneStepIterator(iter2, 13);
                iterator2.setRoot(iter2.getStartNode(), xctxt);
                XObject result5 = new XNodeSet(iterator2);
                return result5;
            } catch (Exception ex2) {
                throw new WrappedRuntimeException(ex2);
            }
        }
        if (obj instanceof DTMIterator) {
            XObject result6 = new XNodeSet((DTMIterator) obj);
            return result6;
        }
        if (obj instanceof Node) {
            XObject result7 = new XNodeSetForDOM((Node) obj, xctxt);
            return result7;
        }
        if (obj instanceof NodeList) {
            XObject result8 = new XNodeSetForDOM((NodeList) obj, xctxt);
            return result8;
        }
        if (obj instanceof NodeIterator) {
            XObject result9 = new XNodeSetForDOM((NodeIterator) obj, xctxt);
            return result9;
        }
        XObject result10 = new XObject(obj);
        return result10;
    }
}
