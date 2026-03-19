package org.apache.xalan.templates;

import java.util.Hashtable;
import javax.xml.transform.TransformerException;
import org.apache.xalan.transformer.KeyManager;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xml.utils.QName;
import org.apache.xml.utils.XMLString;
import org.apache.xpath.XPathContext;
import org.apache.xpath.axes.UnionPathIterator;
import org.apache.xpath.functions.Function2Args;
import org.apache.xpath.objects.XNodeSet;
import org.apache.xpath.objects.XObject;

public class FuncKey extends Function2Args {
    private static Boolean ISTRUE = new Boolean(true);
    static final long serialVersionUID = 9089293100115347340L;

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        TransformerImpl transformerImpl = (TransformerImpl) xPathContext.getOwnerObject();
        int currentNode = xPathContext.getCurrentNode();
        int documentRoot = xPathContext.getDTM(currentNode).getDocumentRoot(currentNode);
        QName qName = new QName(getArg0().execute(xPathContext).str(), xPathContext.getNamespaceContext());
        XObject xObjectExecute = getArg1().execute(xPathContext);
        boolean z = 4 == xObjectExecute.getType();
        KeyManager keyManager = transformerImpl.getKeyManager();
        if (z) {
            XNodeSet xNodeSet = (XNodeSet) xObjectExecute;
            xNodeSet.setShouldCacheNodes(true);
            if (xNodeSet.getLength() <= 1) {
                z = false;
            }
        }
        if (z) {
            Hashtable hashtable = null;
            DTMIterator dTMIteratorIter = xObjectExecute.iter();
            UnionPathIterator unionPathIterator = new UnionPathIterator();
            unionPathIterator.exprSetParent(this);
            while (true) {
                int iNextNode = dTMIteratorIter.nextNode();
                if (-1 != iNextNode) {
                    XMLString stringValue = xPathContext.getDTM(iNextNode).getStringValue(iNextNode);
                    if (stringValue != null) {
                        if (hashtable == null) {
                            hashtable = new Hashtable();
                        }
                        Hashtable hashtable2 = hashtable;
                        if (hashtable2.get(stringValue) == null) {
                            hashtable2.put(stringValue, ISTRUE);
                            XNodeSet nodeSetDTMByKey = keyManager.getNodeSetDTMByKey(xPathContext, documentRoot, qName, stringValue, xPathContext.getNamespaceContext());
                            nodeSetDTMByKey.setRoot(xPathContext.getCurrentNode(), xPathContext);
                            unionPathIterator.addIterator(nodeSetDTMByKey);
                        }
                        hashtable = hashtable2;
                    }
                } else {
                    unionPathIterator.setRoot(xPathContext.getCurrentNode(), xPathContext);
                    return new XNodeSet(unionPathIterator);
                }
            }
        } else {
            XNodeSet nodeSetDTMByKey2 = keyManager.getNodeSetDTMByKey(xPathContext, documentRoot, qName, xObjectExecute.xstr(), xPathContext.getNamespaceContext());
            nodeSetDTMByKey2.setRoot(xPathContext.getCurrentNode(), xPathContext);
            return nodeSetDTMByKey2;
        }
    }
}
