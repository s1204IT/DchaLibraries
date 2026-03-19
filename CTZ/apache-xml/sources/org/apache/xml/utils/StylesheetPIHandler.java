package org.apache.xml.utils;

import java.util.StringTokenizer;
import java.util.Vector;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class StylesheetPIHandler extends DefaultHandler {
    String m_baseID;
    String m_charset;
    String m_media;
    Vector m_stylesheets = new Vector();
    String m_title;
    URIResolver m_uriResolver;

    public void setURIResolver(URIResolver uRIResolver) {
        this.m_uriResolver = uRIResolver;
    }

    public URIResolver getURIResolver() {
        return this.m_uriResolver;
    }

    public StylesheetPIHandler(String str, String str2, String str3, String str4) {
        this.m_baseID = str;
        this.m_media = str2;
        this.m_title = str3;
        this.m_charset = str4;
    }

    public Source getAssociatedStylesheet() {
        int size = this.m_stylesheets.size();
        if (size > 0) {
            return (Source) this.m_stylesheets.elementAt(size - 1);
        }
        return null;
    }

    @Override
    public void processingInstruction(String str, String str2) throws SAXException {
        boolean z;
        String str3;
        if (str.equals("xml-stylesheet")) {
            StringTokenizer stringTokenizer = new StringTokenizer(str2, " \t=\n", true);
            String strSubstring = null;
            String strNextToken = "";
            boolean z2 = false;
            String str4 = null;
            String strSubstring2 = null;
            String strSubstring3 = null;
            String strSubstring4 = null;
            Source sAXSource = null;
            while (stringTokenizer.hasMoreTokens()) {
                if (!z2) {
                    strNextToken = stringTokenizer.nextToken();
                } else {
                    z2 = false;
                }
                if (!stringTokenizer.hasMoreTokens() || (!strNextToken.equals(" ") && !strNextToken.equals("\t") && !strNextToken.equals("="))) {
                    if (strNextToken.equals("type")) {
                        String strNextToken2 = stringTokenizer.nextToken();
                        while (stringTokenizer.hasMoreTokens() && (strNextToken2.equals(" ") || strNextToken2.equals("\t") || strNextToken2.equals("="))) {
                            strNextToken2 = stringTokenizer.nextToken();
                        }
                        strNextToken = strNextToken2;
                        strSubstring = strNextToken2.substring(1, strNextToken2.length() - 1);
                    } else if (strNextToken.equals(org.apache.xalan.templates.Constants.ATTRNAME_HREF)) {
                        strNextToken = stringTokenizer.nextToken();
                        while (stringTokenizer.hasMoreTokens() && (strNextToken.equals(" ") || strNextToken.equals("\t") || strNextToken.equals("="))) {
                            strNextToken = stringTokenizer.nextToken();
                        }
                        if (stringTokenizer.hasMoreTokens()) {
                            z = z2;
                            str3 = strNextToken;
                            strNextToken = stringTokenizer.nextToken();
                            while (strNextToken.equals("=") && stringTokenizer.hasMoreTokens()) {
                                str3 = str3 + strNextToken + stringTokenizer.nextToken();
                                if (!stringTokenizer.hasMoreTokens()) {
                                    break;
                                }
                                strNextToken = stringTokenizer.nextToken();
                                z = true;
                            }
                        } else {
                            z = z2;
                            str3 = strNextToken;
                        }
                        String strSubstring5 = str3.substring(1, str3.length() - 1);
                        try {
                            if (this.m_uriResolver != null) {
                                sAXSource = this.m_uriResolver.resolve(strSubstring5, this.m_baseID);
                            } else {
                                strSubstring5 = SystemIDResolver.getAbsoluteURI(strSubstring5, this.m_baseID);
                                sAXSource = new SAXSource(new InputSource(strSubstring5));
                            }
                            boolean z3 = z;
                            str4 = strSubstring5;
                            z2 = z3;
                        } catch (TransformerException e) {
                            throw new SAXException(e);
                        }
                    } else if (strNextToken.equals("title")) {
                        strNextToken = stringTokenizer.nextToken();
                        while (stringTokenizer.hasMoreTokens() && (strNextToken.equals(" ") || strNextToken.equals("\t") || strNextToken.equals("="))) {
                            strNextToken = stringTokenizer.nextToken();
                        }
                        strSubstring4 = strNextToken.substring(1, strNextToken.length() - 1);
                    } else if (strNextToken.equals("media")) {
                        strNextToken = stringTokenizer.nextToken();
                        while (stringTokenizer.hasMoreTokens() && (strNextToken.equals(" ") || strNextToken.equals("\t") || strNextToken.equals("="))) {
                            strNextToken = stringTokenizer.nextToken();
                        }
                        strSubstring2 = strNextToken.substring(1, strNextToken.length() - 1);
                    } else if (strNextToken.equals("charset")) {
                        strNextToken = stringTokenizer.nextToken();
                        while (stringTokenizer.hasMoreTokens() && (strNextToken.equals(" ") || strNextToken.equals("\t") || strNextToken.equals("="))) {
                            strNextToken = stringTokenizer.nextToken();
                        }
                        strSubstring3 = strNextToken.substring(1, strNextToken.length() - 1);
                    } else if (strNextToken.equals("alternate")) {
                        strNextToken = stringTokenizer.nextToken();
                        while (stringTokenizer.hasMoreTokens() && (strNextToken.equals(" ") || strNextToken.equals("\t") || strNextToken.equals("="))) {
                            strNextToken = stringTokenizer.nextToken();
                        }
                        strNextToken.substring(1, strNextToken.length() - 1).equals("yes");
                    }
                }
            }
            if (strSubstring != null) {
                if ((strSubstring.equals("text/xsl") || strSubstring.equals("text/xml") || strSubstring.equals("application/xml+xslt")) && str4 != null) {
                    if (this.m_media != null && (strSubstring2 == null || !strSubstring2.equals(this.m_media))) {
                        return;
                    }
                    if (this.m_charset != null && (strSubstring3 == null || !strSubstring3.equals(this.m_charset))) {
                        return;
                    }
                    if (this.m_title != null && (strSubstring4 == null || !strSubstring4.equals(this.m_title))) {
                        return;
                    }
                    this.m_stylesheets.addElement(sAXSource);
                }
            }
        }
    }

    @Override
    public void startElement(String str, String str2, String str3, Attributes attributes) throws SAXException {
        throw new StopParseException();
    }

    public void setBaseId(String str) {
        this.m_baseID = str;
    }

    public String getBaseId() {
        return this.m_baseID;
    }
}
