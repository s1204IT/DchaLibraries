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
            return (XObject) obj;
        }
        if (obj instanceof String) {
            return new XString((String) obj);
        }
        if (obj instanceof Boolean) {
            return new XBoolean((Boolean) obj);
        }
        if (obj instanceof Double) {
            return new XNumber((Double) obj);
        }
        return new XObject(obj);
    }

    public static XObject create(Object obj, XPathContext xPathContext) {
        XNodeSetForDOM xNodeSetForDOM;
        XObject xNodeSet;
        if (obj instanceof XObject) {
            xNodeSet = (XObject) obj;
        } else {
            if (obj instanceof String) {
                return new XString((String) obj);
            }
            if (obj instanceof Boolean) {
                return new XBoolean((Boolean) obj);
            }
            if (obj instanceof Number) {
                return new XNumber((Number) obj);
            }
            if (obj instanceof DTM) {
                DTM dtm = (DTM) obj;
                try {
                    int document = dtm.getDocument();
                    DTMAxisIterator axisIterator = dtm.getAxisIterator(13);
                    axisIterator.setStartNode(document);
                    OneStepIterator oneStepIterator = new OneStepIterator(axisIterator, 13);
                    oneStepIterator.setRoot(document, xPathContext);
                    xNodeSet = new XNodeSet(oneStepIterator);
                } catch (Exception e) {
                    throw new WrappedRuntimeException(e);
                }
            } else if (obj instanceof DTMAxisIterator) {
                DTMAxisIterator dTMAxisIterator = (DTMAxisIterator) obj;
                try {
                    OneStepIterator oneStepIterator2 = new OneStepIterator(dTMAxisIterator, 13);
                    oneStepIterator2.setRoot(dTMAxisIterator.getStartNode(), xPathContext);
                    xNodeSet = new XNodeSet(oneStepIterator2);
                } catch (Exception e2) {
                    throw new WrappedRuntimeException(e2);
                }
            } else {
                if (obj instanceof DTMIterator) {
                    return new XNodeSet((DTMIterator) obj);
                }
                if (obj instanceof Node) {
                    xNodeSetForDOM = new XNodeSetForDOM((Node) obj, xPathContext);
                } else if (obj instanceof NodeList) {
                    xNodeSetForDOM = new XNodeSetForDOM((NodeList) obj, xPathContext);
                } else if (obj instanceof NodeIterator) {
                    xNodeSetForDOM = new XNodeSetForDOM((NodeIterator) obj, xPathContext);
                } else {
                    return new XObject(obj);
                }
                return xNodeSetForDOM;
            }
        }
        return xNodeSet;
    }
}
