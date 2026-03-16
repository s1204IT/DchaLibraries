package org.apache.xalan.transformer;

import java.util.Hashtable;
import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xalan.templates.KeyDeclaration;
import org.apache.xml.dtm.DTM;
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

    public KeyTable(int doc, PrefixResolver nscontext, QName name, Vector keyDeclarations, XPathContext xctxt) throws TransformerException {
        this.m_docKey = doc;
        this.m_keyDeclarations = keyDeclarations;
        KeyIterator ki = new KeyIterator(name, keyDeclarations);
        this.m_keyNodes = new XNodeSet(ki);
        this.m_keyNodes.allowDetachToRelease(false);
        this.m_keyNodes.setRoot(doc, xctxt);
    }

    public XNodeSet getNodeSetDTMByKey(QName name, XMLString ref) {
        XNodeSet refNodes = (XNodeSet) getRefsTable().get(ref);
        if (refNodes != null) {
            try {
                refNodes = (XNodeSet) refNodes.cloneWithReset();
            } catch (CloneNotSupportedException e) {
                refNodes = null;
            }
        }
        if (refNodes == null) {
            KeyIterator ki = (KeyIterator) this.m_keyNodes.getContainedIter();
            XPathContext xctxt = ki.getXPathContext();
            XNodeSet refNodes2 = new XNodeSet(xctxt.getDTMManager()) {
                @Override
                public void setRoot(int nodeHandle, Object environment) {
                }
            };
            refNodes2.reset();
            return refNodes2;
        }
        return refNodes;
    }

    public QName getKeyTableName() {
        return getKeyIterator().getName();
    }

    private Vector getKeyDeclarations() {
        int nDeclarations = this.m_keyDeclarations.size();
        Vector keyDecls = new Vector(nDeclarations);
        for (int i = 0; i < nDeclarations; i++) {
            KeyDeclaration kd = (KeyDeclaration) this.m_keyDeclarations.elementAt(i);
            if (kd.getName().equals(getKeyTableName())) {
                keyDecls.add(kd);
            }
        }
        return keyDecls;
    }

    private Hashtable getRefsTable() {
        if (this.m_refsTable == null) {
            this.m_refsTable = new Hashtable(89);
            KeyIterator ki = (KeyIterator) this.m_keyNodes.getContainedIter();
            XPathContext xctxt = ki.getXPathContext();
            Vector keyDecls = getKeyDeclarations();
            int nKeyDecls = keyDecls.size();
            this.m_keyNodes.reset();
            while (true) {
                int currentNode = this.m_keyNodes.nextNode();
                if (-1 == currentNode) {
                    break;
                }
                for (int keyDeclIdx = 0; keyDeclIdx < nKeyDecls; keyDeclIdx++) {
                    try {
                        KeyDeclaration keyDeclaration = (KeyDeclaration) keyDecls.elementAt(keyDeclIdx);
                        XObject xuse = keyDeclaration.getUse().execute(xctxt, currentNode, ki.getPrefixResolver());
                        if (xuse.getType() != 4) {
                            XMLString exprResult = xuse.xstr();
                            addValueInRefsTable(xctxt, exprResult, currentNode);
                        } else {
                            DTMIterator i = ((XNodeSet) xuse).iterRaw();
                            while (true) {
                                int currentNodeInUseClause = i.nextNode();
                                if (-1 == currentNodeInUseClause) {
                                    break;
                                }
                                DTM dtm = xctxt.getDTM(currentNodeInUseClause);
                                XMLString exprResult2 = dtm.getStringValue(currentNodeInUseClause);
                                addValueInRefsTable(xctxt, exprResult2, currentNode);
                            }
                        }
                    } catch (TransformerException te) {
                        throw new WrappedRuntimeException(te);
                    }
                }
            }
        }
        return this.m_refsTable;
    }

    private void addValueInRefsTable(XPathContext xctxt, XMLString ref, int node) {
        XNodeSet nodes = (XNodeSet) this.m_refsTable.get(ref);
        if (nodes == null) {
            XNodeSet nodes2 = new XNodeSet(node, xctxt.getDTMManager());
            nodes2.nextNode();
            this.m_refsTable.put(ref, nodes2);
        } else if (nodes.getCurrentNode() != node) {
            nodes.mutableNodeset().addNode(node);
            nodes.nextNode();
        }
    }
}
