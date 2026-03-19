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

    public void setRawName(String str) {
        this.m_rawName = str;
    }

    public String getName() {
        return this.m_name;
    }

    public void setName(String str) {
        this.m_name = str;
    }

    public String getURI() {
        return this.m_uri;
    }

    public void setURI(String str) {
        this.m_uri = str;
    }

    public AVT(StylesheetHandler stylesheetHandler, String str, String str2, String str3, String str4, ElemTemplateElement elemTemplateElement) throws TransformerException {
        ?? r14;
        ?? NextToken;
        char c;
        Object[] objArr;
        Object[] objArr2;
        ?? r0;
        ?? r142;
        Object[] objArr3;
        Object[] objArr4;
        ?? NextToken2;
        ?? NextToken3;
        Object[] objArr5;
        Object obj;
        Object[] objArr6 = null;
        this.m_simpleString = null;
        this.m_parts = null;
        this.m_uri = str;
        this.m_name = str2;
        this.m_rawName = str3;
        StringTokenizer stringTokenizer = new StringTokenizer(str4, "{}\"'", true);
        int iCountTokens = stringTokenizer.countTokens();
        char c2 = 2;
        if (iCountTokens < 2) {
            this.m_simpleString = str4;
        } else {
            ?? fastStringBuffer = new FastStringBuffer(6);
            ?? fastStringBuffer2 = new FastStringBuffer(6);
            this.m_parts = new Vector(iCountTokens + 1);
            ?? r02 = 0;
            String strCreateMessage = null;
            while (true) {
                if (!stringTokenizer.hasMoreTokens()) {
                    break;
                }
                if (r02 != 0) {
                    NextToken = r02;
                    r14 = objArr6;
                } else {
                    r14 = r02;
                    NextToken = stringTokenizer.nextToken();
                }
                if (NextToken.length() == 1) {
                    char cCharAt = NextToken.charAt(0);
                    if (cCharAt == '\"') {
                        c = c2;
                        objArr3 = objArr6;
                    } else if (cCharAt != '\'') {
                        char c3 = '}';
                        if (cCharAt == '{') {
                            try {
                                NextToken3 = stringTokenizer.nextToken();
                                try {
                                } catch (NoSuchElementException e) {
                                    objArr4 = objArr6;
                                }
                            } catch (NoSuchElementException e2) {
                                objArr4 = objArr6;
                                NextToken2 = r14;
                            }
                            if (NextToken3.equals("{")) {
                                try {
                                    fastStringBuffer.append(NextToken3);
                                    Object[] objArr7 = objArr6;
                                    objArr5 = objArr7;
                                    obj = objArr7;
                                    c = 2;
                                    r0 = obj;
                                    objArr2 = objArr5;
                                } catch (NoSuchElementException e3) {
                                    objArr4 = objArr6;
                                    NextToken2 = NextToken3;
                                    c = 2;
                                    strCreateMessage = XSLMessages.createMessage(XSLTErrorResources.ER_ILLEGAL_ATTRIBUTE_VALUE, new Object[]{str2, str4});
                                    objArr = objArr4;
                                    r142 = NextToken2;
                                    r0 = r142;
                                    objArr2 = objArr;
                                }
                                if (strCreateMessage == null) {
                                }
                            } else if (fastStringBuffer.length() > 0) {
                                try {
                                    this.m_parts.addElement(new AVTPartSimple(fastStringBuffer.toString()));
                                    fastStringBuffer.setLength(0);
                                    try {
                                        fastStringBuffer2.setLength(0);
                                        NextToken2 = NextToken3;
                                        while (NextToken2 != 0) {
                                            try {
                                                if (NextToken2.length() == 1) {
                                                    char cCharAt2 = NextToken2.charAt(0);
                                                    if (cCharAt2 == '\"' || cCharAt2 == '\'') {
                                                        objArr4 = null;
                                                        fastStringBuffer2.append(NextToken2);
                                                        String strNextToken = stringTokenizer.nextToken();
                                                        while (true) {
                                                            NextToken3 = strNextToken;
                                                            try {
                                                                if (NextToken3.equals(NextToken2)) {
                                                                    break;
                                                                }
                                                                fastStringBuffer2.append(NextToken3);
                                                                strNextToken = stringTokenizer.nextToken();
                                                            } catch (NoSuchElementException e4) {
                                                                NextToken2 = NextToken3;
                                                                c = 2;
                                                                strCreateMessage = XSLMessages.createMessage(XSLTErrorResources.ER_ILLEGAL_ATTRIBUTE_VALUE, new Object[]{str2, str4});
                                                                objArr = objArr4;
                                                                r142 = NextToken2;
                                                                r0 = r142;
                                                                objArr2 = objArr;
                                                                if (strCreateMessage == null) {
                                                                }
                                                            }
                                                        }
                                                        fastStringBuffer2.append(NextToken3);
                                                        NextToken2 = stringTokenizer.nextToken();
                                                    } else if (cCharAt2 == '{') {
                                                        objArr4 = null;
                                                        try {
                                                            strCreateMessage = XSLMessages.createMessage(XSLTErrorResources.ER_NO_CURLYBRACE, null);
                                                            NextToken2 = 0;
                                                            NextToken2 = NextToken2;
                                                        } catch (NoSuchElementException e5) {
                                                            c = 2;
                                                            strCreateMessage = XSLMessages.createMessage(XSLTErrorResources.ER_ILLEGAL_ATTRIBUTE_VALUE, new Object[]{str2, str4});
                                                            objArr = objArr4;
                                                            r142 = NextToken2;
                                                            r0 = r142;
                                                            objArr2 = objArr;
                                                            if (strCreateMessage == null) {
                                                            }
                                                        }
                                                    } else if (cCharAt2 != c3) {
                                                        fastStringBuffer2.append(NextToken2);
                                                        NextToken2 = stringTokenizer.nextToken();
                                                    } else {
                                                        fastStringBuffer.setLength(0);
                                                        try {
                                                            this.m_parts.addElement(new AVTPartXPath(stylesheetHandler.createXPath(fastStringBuffer2.toString(), elemTemplateElement)));
                                                            c3 = '}';
                                                            NextToken2 = 0;
                                                            NextToken2 = 0;
                                                        } catch (NoSuchElementException e6) {
                                                            NextToken2 = NextToken2;
                                                            objArr4 = null;
                                                            c = 2;
                                                            strCreateMessage = XSLMessages.createMessage(XSLTErrorResources.ER_ILLEGAL_ATTRIBUTE_VALUE, new Object[]{str2, str4});
                                                            objArr = objArr4;
                                                            r142 = NextToken2;
                                                            r0 = r142;
                                                            objArr2 = objArr;
                                                            if (strCreateMessage == null) {
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    fastStringBuffer2.append(NextToken2);
                                                    NextToken2 = stringTokenizer.nextToken();
                                                }
                                                c3 = '}';
                                            } catch (NoSuchElementException e7) {
                                                NextToken2 = NextToken2;
                                            }
                                        }
                                        objArr2 = null;
                                        if (strCreateMessage == null) {
                                        }
                                        r0 = NextToken2;
                                        c = 2;
                                    } catch (NoSuchElementException e8) {
                                        objArr4 = null;
                                    }
                                } catch (NoSuchElementException e9) {
                                    NextToken2 = NextToken3;
                                    objArr4 = null;
                                    c = 2;
                                    strCreateMessage = XSLMessages.createMessage(XSLTErrorResources.ER_ILLEGAL_ATTRIBUTE_VALUE, new Object[]{str2, str4});
                                    objArr = objArr4;
                                    r142 = NextToken2;
                                    r0 = r142;
                                    objArr2 = objArr;
                                    if (strCreateMessage == null) {
                                    }
                                }
                                if (strCreateMessage == null) {
                                }
                            } else {
                                fastStringBuffer2.setLength(0);
                                NextToken2 = NextToken3;
                                while (NextToken2 != 0) {
                                }
                                objArr2 = null;
                                if (strCreateMessage == null) {
                                }
                                r0 = NextToken2;
                                c = 2;
                                if (strCreateMessage == null) {
                                }
                            }
                        } else if (cCharAt != '}') {
                            fastStringBuffer.append(NextToken);
                            objArr = objArr6;
                            c = 2;
                            r142 = r14;
                        } else {
                            String strNextToken2 = stringTokenizer.nextToken();
                            if (strNextToken2.equals("}")) {
                                fastStringBuffer.append(strNextToken2);
                                Object[] objArr72 = objArr6;
                                objArr5 = objArr72;
                                obj = objArr72;
                                c = 2;
                                r0 = obj;
                                objArr2 = objArr5;
                                if (strCreateMessage == null) {
                                    try {
                                        break;
                                    } catch (SAXException e10) {
                                        throw new TransformerException(e10);
                                    }
                                } else {
                                    objArr6 = objArr2;
                                    c2 = c;
                                    r02 = r0;
                                }
                            } else {
                                try {
                                    stylesheetHandler.warn(XSLTErrorResources.WG_FOUND_CURLYBRACE, objArr6);
                                    fastStringBuffer.append("}");
                                    objArr5 = objArr6;
                                    obj = strNextToken2;
                                    c = 2;
                                    r0 = obj;
                                    objArr2 = objArr5;
                                    if (strCreateMessage == null) {
                                    }
                                } catch (SAXException e11) {
                                    throw new TransformerException(e11);
                                }
                            }
                        }
                    } else {
                        objArr3 = objArr6;
                        c = 2;
                    }
                    fastStringBuffer.append(NextToken);
                    objArr = objArr3;
                    r142 = r14;
                } else {
                    c = c2;
                    objArr = objArr6;
                    fastStringBuffer.append(NextToken);
                    r142 = r14;
                }
                r0 = r142;
                objArr2 = objArr;
                if (strCreateMessage == null) {
                }
            }
            if (fastStringBuffer.length() > 0) {
                this.m_parts.addElement(new AVTPartSimple(fastStringBuffer.toString()));
                fastStringBuffer.setLength(0);
            }
        }
        if (this.m_parts == null && this.m_simpleString == null) {
            this.m_simpleString = "";
        }
    }

    public String getSimpleString() {
        if (this.m_simpleString != null) {
            return this.m_simpleString;
        }
        if (this.m_parts != null) {
            FastStringBuffer buffer = getBuffer();
            int size = this.m_parts.size();
            for (int i = 0; i < size; i++) {
                try {
                    buffer.append(((AVTPart) this.m_parts.elementAt(i)).getSimpleString());
                } catch (Throwable th) {
                    buffer.setLength(0);
                    throw th;
                }
            }
            String string = buffer.toString();
            buffer.setLength(0);
            return string;
        }
        return "";
    }

    public String evaluate(XPathContext xPathContext, int i, PrefixResolver prefixResolver) throws TransformerException {
        if (this.m_simpleString != null) {
            return this.m_simpleString;
        }
        if (this.m_parts != null) {
            FastStringBuffer buffer = getBuffer();
            int size = this.m_parts.size();
            for (int i2 = 0; i2 < size; i2++) {
                try {
                    ((AVTPart) this.m_parts.elementAt(i2)).evaluate(xPathContext, buffer, i, prefixResolver);
                } catch (Throwable th) {
                    buffer.setLength(0);
                    throw th;
                }
            }
            String string = buffer.toString();
            buffer.setLength(0);
            return string;
        }
        return "";
    }

    public boolean isContextInsensitive() {
        return this.m_simpleString != null;
    }

    public boolean canTraverseOutsideSubtree() {
        if (this.m_parts != null) {
            int size = this.m_parts.size();
            for (int i = 0; i < size; i++) {
                if (((AVTPart) this.m_parts.elementAt(i)).canTraverseOutsideSubtree()) {
                    return true;
                }
            }
        }
        return false;
    }

    public void fixupVariables(Vector vector, int i) {
        if (this.m_parts != null) {
            int size = this.m_parts.size();
            for (int i2 = 0; i2 < size; i2++) {
                ((AVTPart) this.m_parts.elementAt(i2)).fixupVariables(vector, i);
            }
        }
    }

    @Override
    public void callVisitors(XSLTVisitor xSLTVisitor) {
        if (xSLTVisitor.visitAVT(this) && this.m_parts != null) {
            int size = this.m_parts.size();
            for (int i = 0; i < size; i++) {
                ((AVTPart) this.m_parts.elementAt(i)).callVisitors(xSLTVisitor);
            }
        }
    }

    public boolean isSimple() {
        return this.m_simpleString != null;
    }

    private final FastStringBuffer getBuffer() {
        return new FastStringBuffer(8);
    }
}
