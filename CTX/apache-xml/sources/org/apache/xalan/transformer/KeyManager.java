package org.apache.xalan.transformer;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xalan.templates.ElemTemplateElement;
import org.apache.xml.utils.PrefixResolver;
import org.apache.xml.utils.QName;
import org.apache.xml.utils.XMLString;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XNodeSet;

public class KeyManager {
    private transient Vector m_key_tables = null;

    public XNodeSet getNodeSetDTMByKey(XPathContext xPathContext, int i, QName qName, XMLString xMLString, PrefixResolver prefixResolver) throws TransformerException {
        ElemTemplateElement elemTemplateElement = (ElemTemplateElement) prefixResolver;
        XNodeSet xNodeSet = null;
        if (elemTemplateElement == null || elemTemplateElement.getStylesheetRoot().getKeysComposed() == null) {
            return null;
        }
        boolean z = false;
        if (this.m_key_tables == null) {
            this.m_key_tables = new Vector(4);
        } else {
            int size = this.m_key_tables.size();
            XNodeSet nodeSetDTMByKey = null;
            int i2 = 0;
            while (true) {
                if (i2 >= size) {
                    break;
                }
                KeyTable keyTable = (KeyTable) this.m_key_tables.elementAt(i2);
                if (!keyTable.getKeyTableName().equals(qName) || i != keyTable.getDocKey() || (nodeSetDTMByKey = keyTable.getNodeSetDTMByKey(qName, xMLString)) == null) {
                    i2++;
                } else {
                    z = true;
                    break;
                }
            }
            xNodeSet = nodeSetDTMByKey;
        }
        if (xNodeSet == null && !z) {
            KeyTable keyTable2 = new KeyTable(i, prefixResolver, qName, elemTemplateElement.getStylesheetRoot().getKeysComposed(), xPathContext);
            this.m_key_tables.addElement(keyTable2);
            if (i == keyTable2.getDocKey()) {
                return keyTable2.getNodeSetDTMByKey(qName, xMLString);
            }
            return xNodeSet;
        }
        return xNodeSet;
    }
}
