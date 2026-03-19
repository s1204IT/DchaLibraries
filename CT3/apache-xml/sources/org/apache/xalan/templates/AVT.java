package org.apache.xalan.templates;

import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xalan.processor.StylesheetHandler;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xml.utils.FastStringBuffer;
import org.apache.xml.utils.PrefixResolver;
import org.apache.xpath.XPath;
import org.apache.xpath.XPathContext;
import org.xml.sax.SAXException;

public class AVT implements Serializable, XSLTVisitable {
    private static final int INIT_BUFFER_CHUNK_BITS = 8;
    private static final boolean USE_OBJECT_POOL = false;
    static final long serialVersionUID = 5167607155517042691L;
    private String m_name;
    private Vector m_parts;
    private String m_rawName;
    private String m_simpleString;
    private String m_uri;

    public String getRawName() {
        return this.m_rawName;
    }

    public void setRawName(String rawName) {
        this.m_rawName = rawName;
    }

    public String getName() {
        return this.m_name;
    }

    public void setName(String name) {
        this.m_name = name;
    }

    public String getURI() {
        return this.m_uri;
    }

    public void setURI(String uri) {
        this.m_uri = uri;
    }

    public AVT(StylesheetHandler handler, String uri, String name, String rawName, String stringedValue, ElemTemplateElement owner) throws TransformerException {
        String t;
        this.m_simpleString = null;
        this.m_parts = null;
        this.m_uri = uri;
        this.m_name = name;
        this.m_rawName = rawName;
        StringTokenizer tokenizer = new StringTokenizer(stringedValue, "{}\"'", true);
        int nTokens = tokenizer.countTokens();
        if (nTokens < 2) {
            this.m_simpleString = stringedValue;
        } else {
            FastStringBuffer buffer = new FastStringBuffer(6);
            FastStringBuffer exprBuffer = new FastStringBuffer(6);
            try {
                this.m_parts = new Vector(nTokens + 1);
                String lookahead = null;
                String strCreateMessage = null;
                while (true) {
                    if (tokenizer.hasMoreTokens()) {
                        if (lookahead != null) {
                            t = lookahead;
                            lookahead = null;
                        } else {
                            t = tokenizer.nextToken();
                        }
                        if (t.length() == 1) {
                            switch (t.charAt(0)) {
                                case '\"':
                                case '\'':
                                    buffer.append(t);
                                    break;
                                case '{':
                                    try {
                                        lookahead = tokenizer.nextToken();
                                        if (lookahead.equals("{")) {
                                            buffer.append(lookahead);
                                            lookahead = null;
                                        } else {
                                            if (buffer.length() > 0) {
                                                this.m_parts.addElement(new AVTPartSimple(buffer.toString()));
                                                buffer.setLength(0);
                                            }
                                            exprBuffer.setLength(0);
                                            while (lookahead != null) {
                                                if (lookahead.length() == 1) {
                                                    switch (lookahead.charAt(0)) {
                                                        case '\"':
                                                        case '\'':
                                                            exprBuffer.append(lookahead);
                                                            String quote = lookahead;
                                                            String lookahead2 = tokenizer.nextToken();
                                                            while (!lookahead2.equals(quote)) {
                                                                exprBuffer.append(lookahead2);
                                                                lookahead2 = tokenizer.nextToken();
                                                            }
                                                            exprBuffer.append(lookahead2);
                                                            lookahead = tokenizer.nextToken();
                                                            break;
                                                        case '{':
                                                            strCreateMessage = XSLMessages.createMessage(XSLTErrorResources.ER_NO_CURLYBRACE, null);
                                                            lookahead = null;
                                                            break;
                                                        case '}':
                                                            buffer.setLength(0);
                                                            XPath xpath = handler.createXPath(exprBuffer.toString(), owner);
                                                            this.m_parts.addElement(new AVTPartXPath(xpath));
                                                            lookahead = null;
                                                            break;
                                                        default:
                                                            exprBuffer.append(lookahead);
                                                            lookahead = tokenizer.nextToken();
                                                            break;
                                                    }
                                                } else {
                                                    exprBuffer.append(lookahead);
                                                    lookahead = tokenizer.nextToken();
                                                }
                                            }
                                            if (strCreateMessage != null) {
                                            }
                                        }
                                    } catch (NoSuchElementException e) {
                                        strCreateMessage = XSLMessages.createMessage(XSLTErrorResources.ER_ILLEGAL_ATTRIBUTE_VALUE, new Object[]{name, stringedValue});
                                    }
                                    break;
                                case '}':
                                    lookahead = tokenizer.nextToken();
                                    if (lookahead.equals("}")) {
                                        buffer.append(lookahead);
                                        lookahead = null;
                                    } else {
                                        try {
                                            handler.warn(XSLTErrorResources.WG_FOUND_CURLYBRACE, null);
                                            buffer.append("}");
                                        } catch (SAXException se) {
                                            throw new TransformerException(se);
                                        }
                                    }
                                    break;
                                default:
                                    buffer.append(t);
                                    break;
                            }
                        } else {
                            buffer.append(t);
                        }
                        if (strCreateMessage != null) {
                            try {
                                handler.warn(XSLTErrorResources.WG_ATTR_TEMPLATE, new Object[]{strCreateMessage});
                            } catch (SAXException se2) {
                                throw new TransformerException(se2);
                            }
                        }
                    }
                }
                if (buffer.length() > 0) {
                    this.m_parts.addElement(new AVTPartSimple(buffer.toString()));
                    buffer.setLength(0);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        if (this.m_parts != null || this.m_simpleString != null) {
            return;
        }
        this.m_simpleString = "";
    }

    public String getSimpleString() {
        if (this.m_simpleString != null) {
            return this.m_simpleString;
        }
        if (this.m_parts != null) {
            FastStringBuffer buf = getBuffer();
            int n = this.m_parts.size();
            for (int i = 0; i < n; i++) {
                try {
                    AVTPart part = (AVTPart) this.m_parts.elementAt(i);
                    buf.append(part.getSimpleString());
                } finally {
                    buf.setLength(0);
                }
            }
            String out = buf.toString();
            return out;
        }
        return "";
    }

    public String evaluate(XPathContext xctxt, int context, PrefixResolver nsNode) throws TransformerException {
        if (this.m_simpleString != null) {
            return this.m_simpleString;
        }
        if (this.m_parts != null) {
            FastStringBuffer buf = getBuffer();
            int n = this.m_parts.size();
            for (int i = 0; i < n; i++) {
                try {
                    AVTPart part = (AVTPart) this.m_parts.elementAt(i);
                    part.evaluate(xctxt, buf, context, nsNode);
                } finally {
                    buf.setLength(0);
                }
            }
            String out = buf.toString();
            return out;
        }
        return "";
    }

    public boolean isContextInsensitive() {
        return this.m_simpleString != null;
    }

    public boolean canTraverseOutsideSubtree() {
        if (this.m_parts != null) {
            int n = this.m_parts.size();
            for (int i = 0; i < n; i++) {
                AVTPart part = (AVTPart) this.m_parts.elementAt(i);
                if (part.canTraverseOutsideSubtree()) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    public void fixupVariables(Vector vars, int globalsSize) {
        if (this.m_parts == null) {
            return;
        }
        int n = this.m_parts.size();
        for (int i = 0; i < n; i++) {
            AVTPart part = (AVTPart) this.m_parts.elementAt(i);
            part.fixupVariables(vars, globalsSize);
        }
    }

    @Override
    public void callVisitors(XSLTVisitor visitor) {
        if (!visitor.visitAVT(this) || this.m_parts == null) {
            return;
        }
        int n = this.m_parts.size();
        for (int i = 0; i < n; i++) {
            AVTPart part = (AVTPart) this.m_parts.elementAt(i);
            part.callVisitors(visitor);
        }
    }

    public boolean isSimple() {
        return this.m_simpleString != null;
    }

    private final FastStringBuffer getBuffer() {
        return new FastStringBuffer(8);
    }
}
