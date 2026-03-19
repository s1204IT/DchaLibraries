package org.apache.xalan.transformer;

import java.text.Collator;
import java.util.Locale;
import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xml.utils.PrefixResolver;
import org.apache.xpath.XPath;

class NodeSortKey {
    boolean m_caseOrderUpper;
    Collator m_col;
    boolean m_descending;
    Locale m_locale;
    PrefixResolver m_namespaceContext;
    TransformerImpl m_processor;
    XPath m_selectPat;
    boolean m_treatAsNumbers;

    NodeSortKey(TransformerImpl transformerImpl, XPath xPath, boolean z, boolean z2, String str, boolean z3, PrefixResolver prefixResolver) throws TransformerException {
        this.m_processor = transformerImpl;
        this.m_namespaceContext = prefixResolver;
        this.m_selectPat = xPath;
        this.m_treatAsNumbers = z;
        this.m_descending = z2;
        this.m_caseOrderUpper = z3;
        if (str != null && !this.m_treatAsNumbers) {
            this.m_locale = new Locale(str.toLowerCase(), Locale.getDefault().getCountry());
            if (this.m_locale == null) {
                this.m_locale = Locale.getDefault();
            }
        } else {
            this.m_locale = Locale.getDefault();
        }
        this.m_col = Collator.getInstance(this.m_locale);
        if (this.m_col == null) {
            this.m_processor.getMsgMgr().warn(null, XSLTErrorResources.WG_CANNOT_FIND_COLLATOR, new Object[]{str});
            this.m_col = Collator.getInstance();
        }
    }
}
