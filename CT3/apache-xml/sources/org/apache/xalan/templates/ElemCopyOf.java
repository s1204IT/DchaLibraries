package org.apache.xalan.templates;

import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.serialize.SerializerUtils;
import org.apache.xalan.templates.StylesheetRoot;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xalan.transformer.TreeWalker2Result;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xml.dtm.ref.DTMTreeWalker;
import org.apache.xml.serializer.SerializationHandler;
import org.apache.xpath.XPath;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XObject;
import org.xml.sax.SAXException;

public class ElemCopyOf extends ElemTemplateElement {
    static final long serialVersionUID = -7433828829497411127L;
    public XPath m_selectExpression = null;

    public void setSelect(XPath expr) {
        this.m_selectExpression = expr;
    }

    public XPath getSelect() {
        return this.m_selectExpression;
    }

    @Override
    public void compose(StylesheetRoot sroot) throws TransformerException {
        super.compose(sroot);
        StylesheetRoot.ComposeState cstate = sroot.getComposeState();
        this.m_selectExpression.fixupVariables(cstate.getVariableNames(), cstate.getGlobalsSize());
    }

    @Override
    public int getXSLToken() {
        return 74;
    }

    @Override
    public String getNodeName() {
        return Constants.ELEMNAME_COPY_OF_STRING;
    }

    @Override
    public void execute(TransformerImpl transformer) throws TransformerException {
        try {
            XPathContext xctxt = transformer.getXPathContext();
            int sourceNode = xctxt.getCurrentNode();
            XObject value = this.m_selectExpression.execute(xctxt, sourceNode, this);
            SerializationHandler handler = transformer.getSerializationHandler();
            if (value == null) {
                return;
            }
            int type = value.getType();
            switch (type) {
                case 1:
                case 2:
                case 3:
                    String s = value.str();
                    handler.characters(s.toCharArray(), 0, s.length());
                    return;
                case 4:
                    DTMIterator nl = value.iter();
                    DTMTreeWalker tw = new TreeWalker2Result(transformer, handler);
                    while (true) {
                        int pos = nl.nextNode();
                        if (-1 == pos) {
                            return;
                        }
                        DTM dtm = xctxt.getDTMManager().getDTM(pos);
                        short t = dtm.getNodeType(pos);
                        if (t == 9) {
                            for (int child = dtm.getFirstChild(pos); child != -1; child = dtm.getNextSibling(child)) {
                                tw.traverse(child);
                            }
                        } else if (t == 2) {
                            SerializerUtils.addAttribute(handler, pos);
                        } else {
                            tw.traverse(pos);
                        }
                    }
                    break;
                case 5:
                    SerializerUtils.outputResultTreeFragment(handler, value, transformer.getXPathContext());
                    return;
                default:
                    String s2 = value.str();
                    handler.characters(s2.toCharArray(), 0, s2.length());
                    return;
            }
        } catch (SAXException se) {
            throw new TransformerException(se);
        }
    }

    @Override
    public ElemTemplateElement appendChild(ElemTemplateElement newChild) {
        error(XSLTErrorResources.ER_CANNOT_ADD, new Object[]{newChild.getNodeName(), getNodeName()});
        return null;
    }

    @Override
    protected void callChildVisitors(XSLTVisitor visitor, boolean callAttrs) {
        if (callAttrs) {
            this.m_selectExpression.getExpression().callVisitors(this.m_selectExpression, visitor);
        }
        super.callChildVisitors(visitor, callAttrs);
    }
}
