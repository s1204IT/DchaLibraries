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
import org.apache.xml.dtm.DTM;
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
    public XObject execute(XPathContext xctxt) throws TransformerException {
        int context = xctxt.getCurrentNode();
        DTM dtm = xctxt.getDTM(context);
        int docContext = dtm.getDocumentRoot(context);
        XObject arg = getArg0().execute(xctxt);
        String base = "";
        Expression arg1Expr = getArg1();
        if (arg1Expr != null) {
            XObject arg2 = arg1Expr.execute(xctxt);
            if (4 == arg2.getType()) {
                int baseNode = arg2.iter().nextNode();
                if (baseNode == -1) {
                    warn(xctxt, XSLTErrorResources.WG_EMPTY_SECOND_ARG, null);
                    return new XNodeSet(xctxt.getDTMManager());
                }
                DTM baseDTM = xctxt.getDTM(baseNode);
                base = baseDTM.getDocumentBaseURI();
            } else {
                arg2.iter();
            }
        } else {
            assertion(xctxt.getNamespaceContext() != null, "Namespace context can not be null!");
            base = xctxt.getNamespaceContext().getBaseIdentifier();
        }
        XNodeSet nodes = new XNodeSet(xctxt.getDTMManager());
        NodeSetDTM mnl = nodes.mutableNodeset();
        DTMIterator iterator = 4 == arg.getType() ? arg.iter() : null;
        int pos = -1;
        while (true) {
            if (iterator != null) {
                pos = iterator.nextNode();
                if (-1 == pos) {
                    return nodes;
                }
            }
            XMLString ref = iterator != null ? xctxt.getDTM(pos).getStringValue(pos) : arg.xstr();
            if (arg1Expr == null && -1 != pos) {
                DTM baseDTM2 = xctxt.getDTM(pos);
                base = baseDTM2.getDocumentBaseURI();
            }
            if (ref != null) {
                if (-1 == docContext) {
                    error(xctxt, XSLTErrorResources.ER_NO_CONTEXT_OWNERDOC, null);
                }
                int indexOfColon = ref.indexOf(58);
                int indexOfSlash = ref.indexOf(47);
                if (indexOfColon != -1 && indexOfSlash != -1 && indexOfColon < indexOfSlash) {
                    base = null;
                }
                int newDoc = getDoc(xctxt, context, ref.toString(), base);
                if (-1 != newDoc && !mnl.contains(newDoc)) {
                    mnl.addElement(newDoc);
                }
                if (iterator == null || newDoc == -1) {
                    return nodes;
                }
            }
        }
    }

    int getDoc(XPathContext xctxt, int context, String uri, String base) throws TransformerException {
        String string;
        SourceTreeManager treeMgr = xctxt.getSourceTreeManager();
        try {
            Source source = treeMgr.resolveURI(base, uri, xctxt.getSAXLocator());
            int newDoc = treeMgr.getNode(source);
            if (-1 != newDoc) {
                return newDoc;
            }
            if (uri.length() == 0) {
                uri = xctxt.getNamespaceContext().getBaseIdentifier();
                try {
                    source = treeMgr.resolveURI(base, uri, xctxt.getSAXLocator());
                } catch (IOException ioe) {
                    throw new TransformerException(ioe.getMessage(), xctxt.getSAXLocator(), ioe);
                }
            }
            String diagnosticsString = null;
            if (uri != null) {
                try {
                    if (uri.length() > 0) {
                        newDoc = treeMgr.getSourceTree(source, xctxt.getSAXLocator(), xctxt);
                    } else {
                        Object[] objArr = new Object[1];
                        objArr[0] = (base == null ? "" : base) + uri;
                        warn(xctxt, "WG_CANNOT_MAKE_URL_FROM", objArr);
                    }
                } catch (Throwable th) {
                    throwable = th;
                    newDoc = -1;
                    while (throwable instanceof WrappedRuntimeException) {
                        throwable = ((WrappedRuntimeException) throwable).getException();
                    }
                    if ((throwable instanceof NullPointerException) || (throwable instanceof ClassCastException)) {
                        throw new WrappedRuntimeException((Exception) throwable);
                    }
                    StringWriter sw = new StringWriter();
                    PrintWriter diagnosticsWriter = new PrintWriter(sw);
                    if (throwable instanceof TransformerException) {
                        TransformerException spe = (TransformerException) throwable;
                        Throwable e = spe;
                        while (e != null) {
                            if (e.getMessage() != null) {
                                diagnosticsWriter.println(" (" + e.getClass().getName() + "): " + e.getMessage());
                            }
                            if (e instanceof TransformerException) {
                                TransformerException spe2 = (TransformerException) e;
                                SourceLocator locator = spe2.getLocator();
                                if (locator != null && locator.getSystemId() != null) {
                                    diagnosticsWriter.println("   ID: " + locator.getSystemId() + " Line #" + locator.getLineNumber() + " Column #" + locator.getColumnNumber());
                                }
                                e = spe2.getException();
                                if (e instanceof WrappedRuntimeException) {
                                    e = ((WrappedRuntimeException) e).getException();
                                }
                            } else {
                                e = null;
                            }
                        }
                    } else {
                        diagnosticsWriter.println(" (" + throwable.getClass().getName() + "): " + throwable.getMessage());
                    }
                    diagnosticsString = throwable.getMessage();
                }
            }
            if (-1 == newDoc) {
                if (diagnosticsString != null) {
                    warn(xctxt, XSLTErrorResources.WG_CANNOT_LOAD_REQUESTED_DOC, new Object[]{diagnosticsString});
                } else {
                    Object[] objArr2 = new Object[1];
                    if (uri == null) {
                        StringBuilder sb = new StringBuilder();
                        if (base == null) {
                            base = "";
                        }
                        string = sb.append(base).append(uri).toString();
                    } else {
                        string = uri.toString();
                    }
                    objArr2[0] = string;
                    warn(xctxt, XSLTErrorResources.WG_CANNOT_LOAD_REQUESTED_DOC, objArr2);
                }
            }
            return newDoc;
        } catch (IOException ioe2) {
            throw new TransformerException(ioe2.getMessage(), xctxt.getSAXLocator(), ioe2);
        } catch (TransformerException te) {
            throw new TransformerException(te);
        }
    }

    @Override
    public void error(XPathContext xctxt, String msg, Object[] args) throws TransformerException {
        String formattedMsg = XSLMessages.createMessage(msg, args);
        ErrorListener errHandler = xctxt.getErrorListener();
        TransformerException spe = new TransformerException(formattedMsg, xctxt.getSAXLocator());
        if (errHandler != null) {
            errHandler.error(spe);
        } else {
            System.out.println(formattedMsg);
        }
    }

    @Override
    public void warn(XPathContext xctxt, String msg, Object[] args) throws TransformerException {
        String formattedMsg = XSLMessages.createWarning(msg, args);
        ErrorListener errHandler = xctxt.getErrorListener();
        TransformerException spe = new TransformerException(formattedMsg, xctxt.getSAXLocator());
        if (errHandler != null) {
            errHandler.warning(spe);
        } else {
            System.out.println(formattedMsg);
        }
    }

    @Override
    public void checkNumberArgs(int argNum) throws WrongNumberArgsException {
        if (argNum < 1 || argNum > 2) {
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
