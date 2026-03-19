package org.apache.xalan.templates;

import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.serialize.SerializerUtils;
import org.apache.xalan.templates.StylesheetRoot;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xalan.transformer.TreeWalker2Result;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xml.serializer.SerializationHandler;
import org.apache.xpath.XPath;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XObject;
import org.xml.sax.SAXException;

public class ElemCopyOf extends ElemTemplateElement {
    static final long serialVersionUID = -7433828829497411127L;
    public XPath m_selectExpression = null;

    public void setSelect(XPath xPath) {
        this.m_selectExpression = xPath;
    }

    public XPath getSelect() {
        return this.m_selectExpression;
    }

    @Override
    public void compose(StylesheetRoot stylesheetRoot) throws TransformerException {
        super.compose(stylesheetRoot);
        StylesheetRoot.ComposeState composeState = stylesheetRoot.getComposeState();
        this.m_selectExpression.fixupVariables(composeState.getVariableNames(), composeState.getGlobalsSize());
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
    public void execute(TransformerImpl transformerImpl) throws TransformerException {
        try {
            XPathContext xPathContext = transformerImpl.getXPathContext();
            XObject xObjectExecute = this.m_selectExpression.execute(xPathContext, xPathContext.getCurrentNode(), this);
            SerializationHandler serializationHandler = transformerImpl.getSerializationHandler();
            if (xObjectExecute != null) {
                switch (xObjectExecute.getType()) {
                    case 1:
                    case 2:
                    case 3:
                        String str = xObjectExecute.str();
                        serializationHandler.characters(str.toCharArray(), 0, str.length());
                        return;
                    case 4:
                        DTMIterator dTMIteratorIter = xObjectExecute.iter();
                        TreeWalker2Result treeWalker2Result = new TreeWalker2Result(transformerImpl, serializationHandler);
                        while (true) {
                            int iNextNode = dTMIteratorIter.nextNode();
                            if (-1 != iNextNode) {
                                DTM dtm = xPathContext.getDTMManager().getDTM(iNextNode);
                                short nodeType = dtm.getNodeType(iNextNode);
                                if (nodeType == 9) {
                                    for (int firstChild = dtm.getFirstChild(iNextNode); firstChild != -1; firstChild = dtm.getNextSibling(firstChild)) {
                                        treeWalker2Result.traverse(firstChild);
                                    }
                                } else if (nodeType == 2) {
                                    SerializerUtils.addAttribute(serializationHandler, iNextNode);
                                } else {
                                    treeWalker2Result.traverse(iNextNode);
                                }
                            } else {
                                return;
                            }
                        }
                        break;
                    case 5:
                        SerializerUtils.outputResultTreeFragment(serializationHandler, xObjectExecute, transformerImpl.getXPathContext());
                        return;
                    default:
                        String str2 = xObjectExecute.str();
                        serializationHandler.characters(str2.toCharArray(), 0, str2.length());
                        return;
                }
            }
        } catch (SAXException e) {
            throw new TransformerException(e);
        }
    }

    @Override
    public ElemTemplateElement appendChild(ElemTemplateElement elemTemplateElement) {
        error(XSLTErrorResources.ER_CANNOT_ADD, new Object[]{elemTemplateElement.getNodeName(), getNodeName()});
        return null;
    }

    @Override
    protected void callChildVisitors(XSLTVisitor xSLTVisitor, boolean z) {
        if (z) {
            this.m_selectExpression.getExpression().callVisitors(this.m_selectExpression, xSLTVisitor);
        }
        super.callChildVisitors(xSLTVisitor, z);
    }
}
