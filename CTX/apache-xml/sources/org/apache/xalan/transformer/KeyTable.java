package org.apache.xalan.transformer;

import java.util.Hashtable;
import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xalan.templates.KeyDeclaration;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xml.utils.PrefixResolver;
import org.apache.xml.utils.QName;
import org.apache.xml.utils.WrappedRuntimeException;
import org.apache.xml.utils.XMLString;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XNodeSet;
import org.apache.xpath.objects.XObject;

public class KeyTable {
    private int m_docKey;
    private Vector m_keyDeclarations;
    private XNodeSet m_keyNodes;
    private Hashtable m_refsTable = null;

    public int getDocKey() {
        return this.m_docKey;
    }

    KeyIterator getKeyIterator() {
        return (KeyIterator) this.m_keyNodes.getContainedIter();
    }

    public KeyTable(int i, PrefixResolver prefixResolver, QName qName, Vector vector, XPathContext xPathContext) throws TransformerException {
        this.m_docKey = i;
        this.m_keyDeclarations = vector;
        this.m_keyNodes = new XNodeSet(new KeyIterator(qName, vector));
        this.m_keyNodes.allowDetachToRelease(false);
        this.m_keyNodes.setRoot(i, xPathContext);
    }

    public XNodeSet getNodeSetDTMByKey(QName qName, XMLString xMLString) {
        XNodeSet xNodeSet = (XNodeSet) getRefsTable().get(xMLString);
        if (xNodeSet != null) {
            try {
                xNodeSet = (XNodeSet) xNodeSet.cloneWithReset();
            } catch (CloneNotSupportedException e) {
                xNodeSet = null;
            }
        }
        if (xNodeSet != null) {
            return xNodeSet;
        }
        XNodeSet xNodeSet2 = new XNodeSet(((KeyIterator) this.m_keyNodes.getContainedIter()).getXPathContext().getDTMManager()) {
            @Override
            public void setRoot(int i, Object obj) {
            }
        };
        xNodeSet2.reset();
        return xNodeSet2;
    }

    public QName getKeyTableName() {
        return getKeyIterator().getName();
    }

    private Vector getKeyDeclarations() {
        int size = this.m_keyDeclarations.size();
        Vector vector = new Vector(size);
        for (int i = 0; i < size; i++) {
            KeyDeclaration keyDeclaration = (KeyDeclaration) this.m_keyDeclarations.elementAt(i);
            if (keyDeclaration.getName().equals(getKeyTableName())) {
                vector.add(keyDeclaration);
            }
        }
        return vector;
    }

    private Hashtable getRefsTable() {
        if (this.m_refsTable == null) {
            this.m_refsTable = new Hashtable(89);
            KeyIterator keyIterator = (KeyIterator) this.m_keyNodes.getContainedIter();
            XPathContext xPathContext = keyIterator.getXPathContext();
            Vector keyDeclarations = getKeyDeclarations();
            int size = keyDeclarations.size();
            this.m_keyNodes.reset();
            while (true) {
                int iNextNode = this.m_keyNodes.nextNode();
                if (-1 == iNextNode) {
                    break;
                }
                for (int i = 0; i < size; i++) {
                    try {
                        XObject xObjectExecute = ((KeyDeclaration) keyDeclarations.elementAt(i)).getUse().execute(xPathContext, iNextNode, keyIterator.getPrefixResolver());
                        if (xObjectExecute.getType() != 4) {
                            addValueInRefsTable(xPathContext, xObjectExecute.xstr(), iNextNode);
                        } else {
                            DTMIterator dTMIteratorIterRaw = ((XNodeSet) xObjectExecute).iterRaw();
                            while (true) {
                                int iNextNode2 = dTMIteratorIterRaw.nextNode();
                                if (-1 == iNextNode2) {
                                    break;
                                }
                                addValueInRefsTable(xPathContext, xPathContext.getDTM(iNextNode2).getStringValue(iNextNode2), iNextNode);
                            }
                        }
                    } catch (TransformerException e) {
                        throw new WrappedRuntimeException(e);
                    }
                }
            }
        }
        return this.m_refsTable;
    }

    private void addValueInRefsTable(XPathContext xPathContext, XMLString xMLString, int i) {
        XNodeSet xNodeSet = (XNodeSet) this.m_refsTable.get(xMLString);
        if (xNodeSet == null) {
            XNodeSet xNodeSet2 = new XNodeSet(i, xPathContext.getDTMManager());
            xNodeSet2.nextNode();
            this.m_refsTable.put(xMLString, xNodeSet2);
        } else if (xNodeSet.getCurrentNode() != i) {
            xNodeSet.mutableNodeset().addNode(i);
            xNodeSet.nextNode();
        }
    }
}
