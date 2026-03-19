package org.apache.xalan.transformer;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.templates.KeyDeclaration;
import org.apache.xml.utils.QName;
import org.apache.xpath.XPathContext;
import org.apache.xpath.axes.OneStepIteratorForward;

public class KeyIterator extends OneStepIteratorForward {
    static final long serialVersionUID = -1349109910100249661L;
    private Vector m_keyDeclarations;
    private QName m_name;

    public QName getName() {
        return this.m_name;
    }

    public Vector getKeyDeclarations() {
        return this.m_keyDeclarations;
    }

    KeyIterator(QName qName, Vector vector) {
        super(16);
        this.m_keyDeclarations = vector;
        this.m_name = qName;
    }

    @Override
    public short acceptNode(int i) {
        boolean z;
        KeyIterator keyIterator = (KeyIterator) this.m_lpi;
        XPathContext xPathContext = keyIterator.getXPathContext();
        Vector keyDeclarations = keyIterator.getKeyDeclarations();
        QName name = keyIterator.getName();
        try {
            int size = keyDeclarations.size();
            z = false;
            for (int i2 = 0; i2 < size; i2++) {
                try {
                    KeyDeclaration keyDeclaration = (KeyDeclaration) keyDeclarations.elementAt(i2);
                    if (keyDeclaration.getName().equals(name)) {
                        try {
                            double matchScore = keyDeclaration.getMatch().getMatchScore(xPathContext, i);
                            keyDeclaration.getMatch();
                            if (matchScore != Double.NEGATIVE_INFINITY) {
                                return (short) 1;
                            }
                            z = true;
                        } catch (TransformerException e) {
                            z = true;
                        }
                    }
                } catch (TransformerException e2) {
                }
            }
        } catch (TransformerException e3) {
            z = false;
        }
        if (!z) {
            throw new RuntimeException(XSLMessages.createMessage(XSLTErrorResources.ER_NO_XSLKEY_DECLARATION, new Object[]{name.getLocalName()}));
        }
        return (short) 2;
    }
}
