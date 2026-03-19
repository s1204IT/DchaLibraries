package mf.org.apache.xml.serialize;

import java.io.UnsupportedEncodingException;

public class OutputFormat {
    private String[] _cdataElements;
    private String _doctypePublic;
    private String _doctypeSystem;
    private String _method;
    private String[] _nonEscapingElements;
    private String _version;
    private int _indent = 0;
    private String _encoding = "UTF-8";
    private EncodingInfo _encodingInfo = null;
    private boolean _allowJavaNames = false;
    private boolean _omitXmlDeclaration = false;
    private boolean _omitDoctype = false;
    private boolean _omitComments = false;
    private boolean _standalone = false;
    private String _lineSeparator = "\n";
    private int _lineWidth = 72;
    private boolean _preserve = false;
    private boolean _preserveEmptyAttributes = false;

    public OutputFormat() {
    }

    public OutputFormat(String method, String encoding, boolean indenting) {
        setMethod(method);
        setEncoding(encoding);
        setIndenting(indenting);
    }

    public void setMethod(String method) {
        this._method = method;
    }

    public String getVersion() {
        return this._version;
    }

    public void setVersion(String version) {
        this._version = version;
    }

    public int getIndent() {
        return this._indent;
    }

    public boolean getIndenting() {
        return this._indent > 0;
    }

    public void setIndenting(boolean on) {
        if (on) {
            this._indent = 4;
            this._lineWidth = 72;
        } else {
            this._indent = 0;
            this._lineWidth = 0;
        }
    }

    public String getEncoding() {
        return this._encoding;
    }

    public void setEncoding(String encoding) {
        this._encoding = encoding;
        this._encodingInfo = null;
    }

    public EncodingInfo getEncodingInfo() throws UnsupportedEncodingException {
        if (this._encodingInfo == null) {
            this._encodingInfo = Encodings.getEncodingInfo(this._encoding, this._allowJavaNames);
        }
        return this._encodingInfo;
    }

    public String getDoctypePublic() {
        return this._doctypePublic;
    }

    public String getDoctypeSystem() {
        return this._doctypeSystem;
    }

    public boolean getOmitComments() {
        return this._omitComments;
    }

    public void setOmitComments(boolean omit) {
        this._omitComments = omit;
    }

    public boolean getOmitDocumentType() {
        return this._omitDoctype;
    }

    public boolean getOmitXMLDeclaration() {
        return this._omitXmlDeclaration;
    }

    public void setOmitXMLDeclaration(boolean omit) {
        this._omitXmlDeclaration = omit;
    }

    public boolean getStandalone() {
        return this._standalone;
    }

    public boolean isCDataElement(String tagName) {
        if (this._cdataElements == null) {
            return false;
        }
        for (int i = 0; i < this._cdataElements.length; i++) {
            if (this._cdataElements[i].equals(tagName)) {
                return true;
            }
        }
        return false;
    }

    public boolean isNonEscapingElement(String tagName) {
        if (this._nonEscapingElements == null) {
            return false;
        }
        for (int i = 0; i < this._nonEscapingElements.length; i++) {
            if (this._nonEscapingElements[i].equals(tagName)) {
                return true;
            }
        }
        return false;
    }

    public String getLineSeparator() {
        return this._lineSeparator;
    }

    public void setLineSeparator(String lineSeparator) {
        if (lineSeparator == null) {
            this._lineSeparator = "\n";
        } else {
            this._lineSeparator = lineSeparator;
        }
    }

    public boolean getPreserveSpace() {
        return this._preserve;
    }

    public int getLineWidth() {
        return this._lineWidth;
    }

    public boolean getPreserveEmptyAttributes() {
        return this._preserveEmptyAttributes;
    }
}
