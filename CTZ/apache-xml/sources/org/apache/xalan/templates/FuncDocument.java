package org.apache.xalan.templates;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xml.utils.WrappedRuntimeException;
import org.apache.xml.utils.XMLString;
import org.apache.xpath.Expression;
import org.apache.xpath.NodeSetDTM;
import org.apache.xpath.SourceTreeManager;
import org.apache.xpath.XPathContext;
import org.apache.xpath.functions.Function2Args;
import org.apache.xpath.functions.WrongNumberArgsException;
import org.apache.xpath.objects.XNodeSet;
import org.apache.xpath.objects.XObject;

public class FuncDocument extends Function2Args {
    static final long serialVersionUID = 2483304325971281424L;

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        DTMIterator dTMIteratorIter;
        int currentNode = xPathContext.getCurrentNode();
        int documentRoot = xPathContext.getDTM(currentNode).getDocumentRoot(currentNode);
        XObject xObjectExecute = getArg0().execute(xPathContext);
        String baseIdentifier = "";
        Expression arg1 = getArg1();
        if (arg1 != null) {
            XObject xObjectExecute2 = arg1.execute(xPathContext);
            if (4 == xObjectExecute2.getType()) {
                int iNextNode = xObjectExecute2.iter().nextNode();
                if (iNextNode == -1) {
                    warn(xPathContext, XSLTErrorResources.WG_EMPTY_SECOND_ARG, null);
                    return new XNodeSet(xPathContext.getDTMManager());
                }
                baseIdentifier = xPathContext.getDTM(iNextNode).getDocumentBaseURI();
            } else {
                xObjectExecute2.iter();
            }
        } else {
            assertion(xPathContext.getNamespaceContext() != null, "Namespace context can not be null!");
            baseIdentifier = xPathContext.getNamespaceContext().getBaseIdentifier();
        }
        XNodeSet xNodeSet = new XNodeSet(xPathContext.getDTMManager());
        NodeSetDTM nodeSetDTMMutableNodeset = xNodeSet.mutableNodeset();
        if (4 == xObjectExecute.getType()) {
            dTMIteratorIter = xObjectExecute.iter();
        } else {
            dTMIteratorIter = null;
        }
        String documentBaseURI = baseIdentifier;
        int iNextNode2 = -1;
        while (true) {
            if (dTMIteratorIter != null) {
                iNextNode2 = dTMIteratorIter.nextNode();
                if (-1 == iNextNode2) {
                    break;
                }
            } else {
                XMLString stringValue = dTMIteratorIter != null ? xPathContext.getDTM(iNextNode2).getStringValue(iNextNode2) : xObjectExecute.xstr();
                if (arg1 == null && -1 != iNextNode2) {
                    documentBaseURI = xPathContext.getDTM(iNextNode2).getDocumentBaseURI();
                }
                if (stringValue != null) {
                    if (-1 == documentRoot) {
                        error(xPathContext, XSLTErrorResources.ER_NO_CONTEXT_OWNERDOC, null);
                    }
                    int iIndexOf = stringValue.indexOf(58);
                    int iIndexOf2 = stringValue.indexOf(47);
                    if (iIndexOf != -1 && iIndexOf2 != -1 && iIndexOf < iIndexOf2) {
                        documentBaseURI = null;
                    }
                    int doc = getDoc(xPathContext, currentNode, stringValue.toString(), documentBaseURI);
                    if (-1 != doc && !nodeSetDTMMutableNodeset.contains(doc)) {
                        nodeSetDTMMutableNodeset.addElement(doc);
                    }
                    if (dTMIteratorIter == null || doc == -1) {
                        break;
                    }
                }
            }
        }
        return xNodeSet;
    }

    int getDoc(XPathContext xPathContext, int i, String str, String str2) throws TransformerException {
        String string;
        SourceTreeManager sourceTreeManager = xPathContext.getSourceTreeManager();
        try {
            Source sourceResolveURI = sourceTreeManager.resolveURI(str2, str, xPathContext.getSAXLocator());
            int node = sourceTreeManager.getNode(sourceResolveURI);
            if (-1 != node) {
                return node;
            }
            if (str.length() == 0) {
                str = xPathContext.getNamespaceContext().getBaseIdentifier();
                try {
                    sourceResolveURI = sourceTreeManager.resolveURI(str2, str, xPathContext.getSAXLocator());
                } catch (IOException e) {
                    throw new TransformerException(e.getMessage(), xPathContext.getSAXLocator(), e);
                }
            }
            String message = null;
            if (str != null) {
                try {
                    if (str.length() > 0) {
                        node = sourceTreeManager.getSourceTree(sourceResolveURI, xPathContext.getSAXLocator(), xPathContext);
                    } else {
                        Object[] objArr = new Object[1];
                        StringBuilder sb = new StringBuilder();
                        sb.append(str2 == null ? "" : str2);
                        sb.append(str);
                        objArr[0] = sb.toString();
                        warn(xPathContext, "WG_CANNOT_MAKE_URL_FROM", objArr);
                    }
                } catch (Throwable th) {
                    th = th;
                    while (th instanceof WrappedRuntimeException) {
                        th = ((WrappedRuntimeException) th).getException();
                    }
                    if ((th instanceof NullPointerException) || (th instanceof ClassCastException)) {
                        throw new WrappedRuntimeException((Exception) th);
                    }
                    PrintWriter printWriter = new PrintWriter(new StringWriter());
                    if (th instanceof TransformerException) {
                        Throwable exception = (TransformerException) th;
                        while (exception != null) {
                            if (exception.getMessage() != null) {
                                printWriter.println(" (" + exception.getClass().getName() + "): " + exception.getMessage());
                            }
                            if (!(exception instanceof TransformerException)) {
                                exception = null;
                            } else {
                                TransformerException transformerException = (TransformerException) exception;
                                SourceLocator locator = transformerException.getLocator();
                                if (locator != null && locator.getSystemId() != null) {
                                    printWriter.println("   ID: " + locator.getSystemId() + " Line #" + locator.getLineNumber() + " Column #" + locator.getColumnNumber());
                                }
                                exception = transformerException.getException();
                                if (exception instanceof WrappedRuntimeException) {
                                    exception = ((WrappedRuntimeException) exception).getException();
                                }
                            }
                        }
                    } else {
                        printWriter.println(" (" + th.getClass().getName() + "): " + th.getMessage());
                    }
                    message = th.getMessage();
                    node = -1;
                }
            }
            if (-1 == node) {
                if (message != null) {
                    warn(xPathContext, XSLTErrorResources.WG_CANNOT_LOAD_REQUESTED_DOC, new Object[]{message});
                } else {
                    Object[] objArr2 = new Object[1];
                    if (str == null) {
                        StringBuilder sb2 = new StringBuilder();
                        if (str2 == null) {
                            str2 = "";
                        }
                        sb2.append(str2);
                        sb2.append(str);
                        string = sb2.toString();
                    } else {
                        string = str.toString();
                    }
                    objArr2[0] = string;
                    warn(xPathContext, XSLTErrorResources.WG_CANNOT_LOAD_REQUESTED_DOC, objArr2);
                }
            }
            return node;
        } catch (IOException e2) {
            throw new TransformerException(e2.getMessage(), xPathContext.getSAXLocator(), e2);
        } catch (TransformerException e3) {
            throw new TransformerException(e3);
        }
    }

    @Override
    public void error(XPathContext xPathContext, String str, Object[] objArr) throws TransformerException {
        String strCreateMessage = XSLMessages.createMessage(str, objArr);
        ErrorListener errorListener = xPathContext.getErrorListener();
        TransformerException transformerException = new TransformerException(strCreateMessage, xPathContext.getSAXLocator());
        if (errorListener != null) {
            errorListener.error(transformerException);
        } else {
            System.out.println(strCreateMessage);
        }
    }

    @Override
    public void warn(XPathContext xPathContext, String str, Object[] objArr) throws TransformerException {
        String strCreateWarning = XSLMessages.createWarning(str, objArr);
        ErrorListener errorListener = xPathContext.getErrorListener();
        TransformerException transformerException = new TransformerException(strCreateWarning, xPathContext.getSAXLocator());
        if (errorListener != null) {
            errorListener.warning(transformerException);
        } else {
            System.out.println(strCreateWarning);
        }
    }

    @Override
    public void checkNumberArgs(int i) throws WrongNumberArgsException {
        if (i < 1 || i > 2) {
            reportWrongNumberArgs();
        }
    }

    @Override
    protected void reportWrongNumberArgs() throws WrongNumberArgsException {
        throw new WrongNumberArgsException(XSLMessages.createMessage(XSLTErrorResources.ER_ONE_OR_TWO, null));
    }

    @Override
    public boolean isNodesetExpr() {
        return true;
    }
}
