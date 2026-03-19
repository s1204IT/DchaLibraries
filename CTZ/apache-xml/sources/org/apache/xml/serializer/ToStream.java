package org.apache.xml.serializer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import org.apache.xalan.templates.Constants;
import org.apache.xml.serializer.NamespaceMappings;
import org.apache.xml.serializer.utils.Utils;
import org.apache.xml.serializer.utils.WrappedRuntimeException;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public abstract class ToStream extends SerializerBase {
    private static final String COMMENT_BEGIN = "<!--";
    private static final String COMMENT_END = "-->";
    private static final char[] s_systemLineSep = SecuritySupport.getInstance().getSystemProperty("line.separator").toCharArray();
    protected CharInfo m_charInfo;
    OutputStream m_outputStream;
    boolean m_startNewLine;
    private boolean m_writer_set_by_user;
    protected BoolStack m_disableOutputEscapingStates = new BoolStack();
    EncodingInfo m_encodingInfo = new EncodingInfo(null, null, 0);
    protected BoolStack m_preserves = new BoolStack();
    protected boolean m_ispreserve = false;
    protected boolean m_isprevtext = false;
    protected char[] m_lineSep = s_systemLineSep;
    protected boolean m_lineSepUse = true;
    protected int m_lineSepLen = this.m_lineSep.length;
    boolean m_shouldFlush = true;
    protected boolean m_spaceBeforeClose = false;
    protected boolean m_inDoctype = false;
    boolean m_isUTF8 = false;
    protected boolean m_cdataStartCalled = false;
    private boolean m_expandDTDEntities = true;
    protected boolean m_escaping = true;

    protected void closeCDATA() throws SAXException {
        try {
            this.m_writer.write(SerializerConstants.CDATA_DELIMITER_CLOSE);
            this.m_cdataTagOpen = false;
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public void serialize(Node node) throws IOException {
        try {
            new TreeWalker(this).traverse(node);
        } catch (SAXException e) {
            throw new WrappedRuntimeException(e);
        }
    }

    protected final void flushWriter() throws SAXException {
        Writer writer = this.m_writer;
        if (writer != null) {
            try {
                if (writer instanceof WriterToUTF8Buffered) {
                    if (this.m_shouldFlush) {
                        ((WriterToUTF8Buffered) writer).flush();
                    } else {
                        ((WriterToUTF8Buffered) writer).flushBuffer();
                    }
                }
                if (writer instanceof WriterToASCI) {
                    if (this.m_shouldFlush) {
                        writer.flush();
                        return;
                    }
                    return;
                }
                writer.flush();
            } catch (IOException e) {
                throw new SAXException(e);
            }
        }
    }

    @Override
    public OutputStream getOutputStream() {
        return this.m_outputStream;
    }

    public void elementDecl(String str, String str2) throws SAXException {
        if (this.m_inExternalDTD) {
            return;
        }
        try {
            Writer writer = this.m_writer;
            DTDprolog();
            writer.write("<!ELEMENT ");
            writer.write(str);
            writer.write(32);
            writer.write(str2);
            writer.write(62);
            writer.write(this.m_lineSep, 0, this.m_lineSepLen);
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    public void internalEntityDecl(String str, String str2) throws SAXException {
        if (this.m_inExternalDTD) {
            return;
        }
        try {
            DTDprolog();
            outputEntityDecl(str, str2);
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    void outputEntityDecl(String str, String str2) throws IOException {
        Writer writer = this.m_writer;
        writer.write("<!ENTITY ");
        writer.write(str);
        writer.write(" \"");
        writer.write(str2);
        writer.write("\">");
        writer.write(this.m_lineSep, 0, this.m_lineSepLen);
    }

    protected final void outputLineSep() throws IOException {
        this.m_writer.write(this.m_lineSep, 0, this.m_lineSepLen);
    }

    @Override
    void setProp(String str, String str2, boolean z) {
        String str3;
        if (str2 != null) {
            char firstCharLocName = getFirstCharLocName(str);
            if (firstCharLocName != 'i') {
                if (firstCharLocName != 'o') {
                    if (firstCharLocName != 's') {
                        if (firstCharLocName != 'v') {
                            switch (firstCharLocName) {
                                case 'c':
                                    if (Constants.ATTRNAME_OUTPUT_CDATA_SECTION_ELEMENTS.equals(str)) {
                                        addCdataSectionElements(str2);
                                    }
                                    break;
                                case 'd':
                                    if (Constants.ATTRNAME_OUTPUT_DOCTYPE_SYSTEM.equals(str)) {
                                        this.m_doctypeSystem = str2;
                                    } else if (Constants.ATTRNAME_OUTPUT_DOCTYPE_PUBLIC.equals(str)) {
                                        this.m_doctypePublic = str2;
                                        if (str2.startsWith("-//W3C//DTD XHTML")) {
                                            this.m_spaceBeforeClose = true;
                                        }
                                    }
                                    break;
                                case 'e':
                                    if ("encoding".equals(str)) {
                                        String mimeEncoding = Encodings.getMimeEncoding(str2);
                                        if (mimeEncoding != null) {
                                            super.setProp("mime-name", mimeEncoding, z);
                                        }
                                        String outputPropertyNonDefault = getOutputPropertyNonDefault("encoding");
                                        String outputPropertyDefault = getOutputPropertyDefault("encoding");
                                        if ((z && (outputPropertyDefault == null || !outputPropertyDefault.equalsIgnoreCase(str2))) || (!z && (outputPropertyNonDefault == null || !outputPropertyNonDefault.equalsIgnoreCase(str2)))) {
                                            EncodingInfo encodingInfo = Encodings.getEncodingInfo(str2);
                                            if (str2 == null || encodingInfo.name != null) {
                                                str3 = str2;
                                            } else {
                                                String strCreateMessage = Utils.messages.createMessage("ER_ENCODING_NOT_SUPPORTED", new Object[]{str2});
                                                String str4 = "Warning: encoding \"" + str2 + "\" not supported, using UTF-8";
                                                try {
                                                    Transformer transformer = super.getTransformer();
                                                    if (transformer != null) {
                                                        ErrorListener errorListener = transformer.getErrorListener();
                                                        if (errorListener != null && this.m_sourceLocator != null) {
                                                            errorListener.warning(new TransformerException(strCreateMessage, this.m_sourceLocator));
                                                            errorListener.warning(new TransformerException(str4, this.m_sourceLocator));
                                                        } else {
                                                            System.out.println(strCreateMessage);
                                                            System.out.println(str4);
                                                        }
                                                    } else {
                                                        System.out.println(strCreateMessage);
                                                        System.out.println(str4);
                                                    }
                                                } catch (Exception e) {
                                                }
                                                str2 = "UTF-8";
                                                str3 = "UTF-8";
                                                encodingInfo = Encodings.getEncodingInfo("UTF-8");
                                            }
                                            if (!z || outputPropertyNonDefault == null) {
                                                this.m_encodingInfo = encodingInfo;
                                                if (str2 != null) {
                                                    this.m_isUTF8 = str2.equals("UTF-8");
                                                }
                                                OutputStream outputStream = getOutputStream();
                                                if (outputStream != null) {
                                                    Writer writer = getWriter();
                                                    String outputProperty = getOutputProperty("encoding");
                                                    if ((writer == null || !this.m_writer_set_by_user) && !str2.equalsIgnoreCase(outputProperty)) {
                                                        super.setProp(str, str3, z);
                                                        setOutputStreamInternal(outputStream, false);
                                                    }
                                                }
                                            }
                                            str2 = str3;
                                        }
                                    }
                                    break;
                                default:
                                    switch (firstCharLocName) {
                                        case 'l':
                                            if (OutputPropertiesFactory.S_KEY_LINE_SEPARATOR.equals(str)) {
                                                this.m_lineSep = str2.toCharArray();
                                                this.m_lineSepLen = this.m_lineSep.length;
                                            }
                                            break;
                                        case 'm':
                                            if (Constants.ATTRNAME_OUTPUT_MEDIATYPE.equals(str)) {
                                                this.m_mediatype = str2;
                                            }
                                            break;
                                    }
                                    break;
                            }
                        } else if ("version".equals(str)) {
                            this.m_version = str2;
                        }
                    } else if (Constants.ATTRNAME_OUTPUT_STANDALONE.equals(str)) {
                        if (z) {
                            setStandaloneInternal(str2);
                        } else {
                            this.m_standaloneWasSpecified = true;
                            setStandaloneInternal(str2);
                        }
                    }
                } else if ("omit-xml-declaration".equals(str)) {
                    this.m_shouldNotWriteXMLHeader = "yes".equals(str2);
                }
            } else if (OutputPropertiesFactory.S_KEY_INDENT_AMOUNT.equals(str)) {
                setIndentAmount(Integer.parseInt(str2));
            } else if ("indent".equals(str)) {
                this.m_doIndent = "yes".equals(str2);
            }
            super.setProp(str, str2, z);
        }
    }

    public void setOutputFormat(Properties properties) {
        boolean z = this.m_shouldFlush;
        if (properties != null) {
            Enumeration<?> enumerationPropertyNames = properties.propertyNames();
            while (enumerationPropertyNames.hasMoreElements()) {
                String str = (String) enumerationPropertyNames.nextElement();
                String property = properties.getProperty(str);
                String str2 = (String) properties.get(str);
                if (str2 == null && property != null) {
                    setOutputPropertyDefault(str, property);
                }
                if (str2 != null) {
                    setOutputProperty(str, str2);
                }
            }
        }
        String str3 = (String) properties.get(OutputPropertiesFactory.S_KEY_ENTITIES);
        if (str3 != null) {
            this.m_charInfo = CharInfo.getCharInfo(str3, (String) properties.get(Constants.ATTRNAME_OUTPUT_METHOD));
        }
        this.m_shouldFlush = z;
    }

    @Override
    public Properties getOutputFormat() {
        Properties properties = new Properties();
        for (String str : getOutputPropDefaultKeys()) {
            properties.put(str, getOutputPropertyDefault(str));
        }
        Properties properties2 = new Properties(properties);
        for (String str2 : getOutputPropKeys()) {
            String outputPropertyNonDefault = getOutputPropertyNonDefault(str2);
            if (outputPropertyNonDefault != null) {
                properties2.put(str2, outputPropertyNonDefault);
            }
        }
        return properties2;
    }

    @Override
    public void setWriter(Writer writer) {
        setWriterInternal(writer, true);
    }

    private void setWriterInternal(Writer writer, boolean z) {
        this.m_writer_set_by_user = z;
        this.m_writer = writer;
        if (this.m_tracer != null) {
            boolean z2 = true;
            Appendable writer2 = this.m_writer;
            while (true) {
                if (!(writer2 instanceof WriterChain)) {
                    break;
                }
                if (writer2 instanceof SerializerTraceWriter) {
                    z2 = false;
                    break;
                }
                writer2 = ((WriterChain) writer2).getWriter();
            }
            if (z2) {
                this.m_writer = new SerializerTraceWriter(this.m_writer, this.m_tracer);
            }
        }
    }

    public boolean setLineSepUse(boolean z) {
        boolean z2 = this.m_lineSepUse;
        this.m_lineSepUse = z;
        return z2;
    }

    @Override
    public void setOutputStream(OutputStream outputStream) {
        setOutputStreamInternal(outputStream, true);
    }

    private void setOutputStreamInternal(OutputStream outputStream, boolean z) {
        Writer writer;
        Writer writer2;
        this.m_outputStream = outputStream;
        String outputProperty = getOutputProperty("encoding");
        if ("UTF-8".equalsIgnoreCase(outputProperty)) {
            setWriterInternal(new WriterToUTF8Buffered(outputStream), false);
            return;
        }
        if ("WINDOWS-1250".equals(outputProperty) || "US-ASCII".equals(outputProperty) || "ASCII".equals(outputProperty)) {
            setWriterInternal(new WriterToASCI(outputStream), false);
            return;
        }
        if (outputProperty != null) {
            try {
                writer = Encodings.getWriter(outputStream, outputProperty);
            } catch (UnsupportedEncodingException e) {
                writer = null;
            }
            if (writer == null) {
                System.out.println("Warning: encoding \"" + outputProperty + "\" not supported, using UTF-8");
                setEncoding("UTF-8");
                try {
                    writer2 = Encodings.getWriter(outputStream, "UTF-8");
                } catch (UnsupportedEncodingException e2) {
                    e2.printStackTrace();
                    writer2 = writer;
                }
            } else {
                writer2 = writer;
            }
            setWriterInternal(writer2, false);
            return;
        }
        setWriterInternal(new OutputStreamWriter(outputStream), false);
    }

    @Override
    public boolean setEscaping(boolean z) {
        boolean z2 = this.m_escaping;
        this.m_escaping = z;
        return z2;
    }

    protected void indent(int i) throws IOException {
        if (this.m_startNewLine) {
            outputLineSep();
        }
        if (this.m_indentAmount > 0) {
            printSpace(i * this.m_indentAmount);
        }
    }

    protected void indent() throws IOException {
        indent(this.m_elemContext.m_currentElemDepth);
    }

    private void printSpace(int i) throws IOException {
        Writer writer = this.m_writer;
        for (int i2 = 0; i2 < i; i2++) {
            writer.write(32);
        }
    }

    public void attributeDecl(String str, String str2, String str3, String str4, String str5) throws SAXException {
        if (this.m_inExternalDTD) {
            return;
        }
        try {
            Writer writer = this.m_writer;
            DTDprolog();
            writer.write("<!ATTLIST ");
            writer.write(str);
            writer.write(32);
            writer.write(str2);
            writer.write(32);
            writer.write(str3);
            if (str4 != null) {
                writer.write(32);
                writer.write(str4);
            }
            writer.write(62);
            writer.write(this.m_lineSep, 0, this.m_lineSepLen);
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public Writer getWriter() {
        return this.m_writer;
    }

    public void externalEntityDecl(String str, String str2, String str3) throws SAXException {
        try {
            DTDprolog();
            this.m_writer.write("<!ENTITY ");
            this.m_writer.write(str);
            if (str2 != null) {
                this.m_writer.write(" PUBLIC \"");
                this.m_writer.write(str2);
            } else {
                this.m_writer.write(" SYSTEM \"");
                this.m_writer.write(str3);
            }
            this.m_writer.write("\" >");
            this.m_writer.write(this.m_lineSep, 0, this.m_lineSepLen);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected boolean escapingNotNeeded(char c) {
        if (c < 127) {
            if (c >= ' ' || '\n' == c || '\r' == c || '\t' == c) {
                return true;
            }
            return false;
        }
        return this.m_encodingInfo.isInEncoding(c);
    }

    protected int writeUTF16Surrogate(char c, char[] cArr, int i, int i2) throws IOException {
        int i3 = i + 1;
        if (i3 >= i2) {
            throw new IOException(Utils.messages.createMessage("ER_INVALID_UTF16_SURROGATE", new Object[]{Integer.toHexString(c)}));
        }
        char c2 = cArr[i3];
        if (!Encodings.isLowUTF16Surrogate(c2)) {
            throw new IOException(Utils.messages.createMessage("ER_INVALID_UTF16_SURROGATE", new Object[]{Integer.toHexString(c) + " " + Integer.toHexString(c2)}));
        }
        Writer writer = this.m_writer;
        if (this.m_encodingInfo.isInEncoding(c, c2)) {
            writer.write(cArr, i, 2);
            return 0;
        }
        if (getEncoding() != null) {
            int codePoint = Encodings.toCodePoint(c, c2);
            writer.write(38);
            writer.write(35);
            writer.write(Integer.toString(codePoint));
            writer.write(59);
            return codePoint;
        }
        writer.write(cArr, i, 2);
        return 0;
    }

    int accumDefaultEntity(Writer writer, char c, int i, char[] cArr, int i2, boolean z, boolean z2) throws IOException {
        String outputStringForChar;
        if (!z2 && '\n' == c) {
            writer.write(this.m_lineSep, 0, this.m_lineSepLen);
        } else if (((z && this.m_charInfo.shouldMapTextChar(c)) || (!z && this.m_charInfo.shouldMapAttrChar(c))) && (outputStringForChar = this.m_charInfo.getOutputStringForChar(c)) != null) {
            writer.write(outputStringForChar);
        } else {
            return i;
        }
        return i + 1;
    }

    void writeNormalizedChars(char[] cArr, int i, int i2, boolean z, boolean z2) throws SAXException, IOException {
        Writer writer = this.m_writer;
        int i3 = i2 + i;
        while (i < i3) {
            char c = cArr[i];
            if ('\n' == c && z2) {
                writer.write(this.m_lineSep, 0, this.m_lineSepLen);
            } else if (z && !escapingNotNeeded(c)) {
                if (this.m_cdataTagOpen) {
                    closeCDATA();
                }
                if (Encodings.isHighUTF16Surrogate(c)) {
                    writeUTF16Surrogate(c, cArr, i, i3);
                    i++;
                } else {
                    writer.write("&#");
                    writer.write(Integer.toString(c));
                    writer.write(59);
                }
            } else if (z && i < i3 - 2 && ']' == c && ']' == cArr[i + 1]) {
                int i4 = i + 2;
                if ('>' == cArr[i4]) {
                    writer.write(SerializerConstants.CDATA_CONTINUE);
                    i = i4;
                }
            } else if (escapingNotNeeded(c)) {
                if (z && !this.m_cdataTagOpen) {
                    writer.write(SerializerConstants.CDATA_DELIMITER_OPEN);
                    this.m_cdataTagOpen = true;
                }
                writer.write(c);
            } else if (Encodings.isHighUTF16Surrogate(c)) {
                if (this.m_cdataTagOpen) {
                    closeCDATA();
                }
                writeUTF16Surrogate(c, cArr, i, i3);
                i++;
            } else {
                if (this.m_cdataTagOpen) {
                    closeCDATA();
                }
                writer.write("&#");
                writer.write(Integer.toString(c));
                writer.write(59);
            }
            i++;
        }
    }

    public void endNonEscaping() throws SAXException {
        this.m_disableOutputEscapingStates.pop();
    }

    public void startNonEscaping() throws SAXException {
        this.m_disableOutputEscapingStates.push(true);
    }

    protected void cdata(char[] cArr, int i, int i2) throws SAXException {
        try {
            boolean z = false;
            if (this.m_elemContext.m_startTagOpen) {
                closeStartTag();
                this.m_elemContext.m_startTagOpen = false;
            }
            this.m_ispreserve = true;
            if (shouldIndent()) {
                indent();
            }
            if (i2 >= 1 && escapingNotNeeded(cArr[i])) {
                z = true;
            }
            if (z && !this.m_cdataTagOpen) {
                this.m_writer.write(SerializerConstants.CDATA_DELIMITER_OPEN);
                this.m_cdataTagOpen = true;
            }
            if (isEscapingDisabled()) {
                charactersRaw(cArr, i, i2);
            } else {
                writeNormalizedChars(cArr, i, i2, true, this.m_lineSepUse);
            }
            if (z && cArr[(i + i2) - 1] == ']') {
                closeCDATA();
            }
            if (this.m_tracer != null) {
                super.fireCDATAEvent(cArr, i, i2);
            }
        } catch (IOException e) {
            throw new SAXException(Utils.messages.createMessage("ER_OIERROR", null), e);
        }
    }

    private boolean isEscapingDisabled() {
        return this.m_disableOutputEscapingStates.peekOrFalse();
    }

    protected void charactersRaw(char[] cArr, int i, int i2) throws SAXException {
        if (this.m_inEntityRef) {
            return;
        }
        try {
            if (this.m_elemContext.m_startTagOpen) {
                closeStartTag();
                this.m_elemContext.m_startTagOpen = false;
            }
            this.m_ispreserve = true;
            this.m_writer.write(cArr, i, i2);
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public void characters(char[] cArr, int i, int i2) throws SAXException {
        if (i2 != 0) {
            if (this.m_inEntityRef && !this.m_expandDTDEntities) {
                return;
            }
            this.m_docIsEmpty = false;
            if (this.m_elemContext.m_startTagOpen) {
                closeStartTag();
                this.m_elemContext.m_startTagOpen = false;
            } else if (this.m_needToCallStartDocument) {
                startDocumentInternal();
            }
            if (this.m_cdataStartCalled || this.m_elemContext.m_isCdataSection) {
                cdata(cArr, i, i2);
                return;
            }
            if (this.m_cdataTagOpen) {
                closeCDATA();
            }
            if (this.m_disableOutputEscapingStates.peekOrFalse() || !this.m_escaping) {
                charactersRaw(cArr, i, i2);
                if (this.m_tracer != null) {
                    super.fireCharEvent(cArr, i, i2);
                    return;
                }
                return;
            }
            if (this.m_elemContext.m_startTagOpen) {
                closeStartTag();
                this.m_elemContext.m_startTagOpen = false;
            }
            int i3 = i + i2;
            int i4 = i - 1;
            try {
                Writer writer = this.m_writer;
                int iProcessLineFeed = i4;
                boolean z = true;
                int i5 = i;
                while (i5 < i3 && z) {
                    char c = cArr[i5];
                    if (this.m_charInfo.shouldMapTextChar(c)) {
                        writeOutCleanChars(cArr, i5, iProcessLineFeed);
                        writer.write(this.m_charInfo.getOutputStringForChar(c));
                        iProcessLineFeed = i5;
                        i5++;
                    } else if (c == '\r') {
                        writeOutCleanChars(cArr, i5, iProcessLineFeed);
                        writer.write("&#13;");
                        iProcessLineFeed = i5;
                        i5++;
                    } else if (c == ' ') {
                        i5++;
                    } else {
                        switch (c) {
                            case '\t':
                                i5++;
                                break;
                            case '\n':
                                iProcessLineFeed = processLineFeed(cArr, i5, iProcessLineFeed, writer);
                                i5++;
                                break;
                        }
                    }
                    z = false;
                }
                if (i5 < i3 || !z) {
                    this.m_ispreserve = true;
                }
                while (i5 < i3) {
                    char c2 = cArr[i5];
                    if (this.m_charInfo.shouldMapTextChar(c2)) {
                        writeOutCleanChars(cArr, i5, iProcessLineFeed);
                        writer.write(this.m_charInfo.getOutputStringForChar(c2));
                    } else {
                        if (c2 <= 31) {
                            if (c2 != '\r') {
                                switch (c2) {
                                    case '\t':
                                        break;
                                    case '\n':
                                        iProcessLineFeed = processLineFeed(cArr, i5, iProcessLineFeed, writer);
                                        break;
                                    default:
                                        writeOutCleanChars(cArr, i5, iProcessLineFeed);
                                        writer.write("&#");
                                        writer.write(Integer.toString(c2));
                                        writer.write(59);
                                        break;
                                }
                            } else {
                                writeOutCleanChars(cArr, i5, iProcessLineFeed);
                                writer.write("&#13;");
                            }
                        } else if (c2 >= 127) {
                            if (c2 <= 159) {
                                writeOutCleanChars(cArr, i5, iProcessLineFeed);
                                writer.write("&#");
                                writer.write(Integer.toString(c2));
                                writer.write(59);
                            } else if (c2 == 8232) {
                                writeOutCleanChars(cArr, i5, iProcessLineFeed);
                                writer.write("&#8232;");
                            } else if (!this.m_encodingInfo.isInEncoding(c2)) {
                                writeOutCleanChars(cArr, i5, iProcessLineFeed);
                                writer.write("&#");
                                writer.write(Integer.toString(c2));
                                writer.write(59);
                            }
                        }
                        i5++;
                    }
                    iProcessLineFeed = i5;
                    i5++;
                }
                int i6 = iProcessLineFeed + 1;
                if (i5 > i6) {
                    this.m_writer.write(cArr, i6, i5 - i6);
                }
                this.m_isprevtext = true;
                if (this.m_tracer != null) {
                    super.fireCharEvent(cArr, i, i2);
                }
            } catch (IOException e) {
                throw new SAXException(e);
            }
        }
    }

    private int processLineFeed(char[] cArr, int i, int i2, Writer writer) throws IOException {
        if (this.m_lineSepUse && (this.m_lineSepLen != 1 || this.m_lineSep[0] != '\n')) {
            writeOutCleanChars(cArr, i, i2);
            writer.write(this.m_lineSep, 0, this.m_lineSepLen);
            return i;
        }
        return i2;
    }

    private void writeOutCleanChars(char[] cArr, int i, int i2) throws IOException {
        int i3 = i2 + 1;
        if (i3 < i) {
            this.m_writer.write(cArr, i3, i - i3);
        }
    }

    private static boolean isCharacterInC0orC1Range(char c) {
        if (c == '\t' || c == '\n' || c == '\r') {
            return false;
        }
        return (c >= 127 && c <= 159) || (c >= 1 && c <= 31);
    }

    private static boolean isNELorLSEPCharacter(char c) {
        return c == 133 || c == 8232;
    }

    private int processDirty(char[] cArr, int i, int i2, char c, int i3, boolean z) throws IOException {
        int i4 = i3 + 1;
        if (i2 > i4) {
            this.m_writer.write(cArr, i4, i2 - i4);
        }
        if ('\n' == c && z) {
            this.m_writer.write(this.m_lineSep, 0, this.m_lineSepLen);
            return i2;
        }
        return accumDefaultEscape(this.m_writer, c, i2, cArr, i, z, false) - 1;
    }

    @Override
    public void characters(String str) throws SAXException {
        if (this.m_inEntityRef && !this.m_expandDTDEntities) {
            return;
        }
        int length = str.length();
        if (length > this.m_charsBuff.length) {
            this.m_charsBuff = new char[(length * 2) + 1];
        }
        str.getChars(0, length, this.m_charsBuff, 0);
        characters(this.m_charsBuff, 0, length);
    }

    private int accumDefaultEscape(Writer writer, char c, int i, char[] cArr, int i2, boolean z, boolean z2) throws IOException {
        int iAccumDefaultEntity = accumDefaultEntity(writer, c, i, cArr, i2, z, z2);
        if (i == iAccumDefaultEntity) {
            if (Encodings.isHighUTF16Surrogate(c)) {
                int i3 = i + 1;
                if (i3 >= i2) {
                    throw new IOException(Utils.messages.createMessage("ER_INVALID_UTF16_SURROGATE", new Object[]{Integer.toHexString(c)}));
                }
                char c2 = cArr[i3];
                if (!Encodings.isLowUTF16Surrogate(c2)) {
                    throw new IOException(Utils.messages.createMessage("ER_INVALID_UTF16_SURROGATE", new Object[]{Integer.toHexString(c) + " " + Integer.toHexString(c2)}));
                }
                int codePoint = Encodings.toCodePoint(c, c2);
                writer.write("&#");
                writer.write(Integer.toString(codePoint));
                writer.write(59);
                return iAccumDefaultEntity + 2;
            }
            if (isCharacterInC0orC1Range(c) || isNELorLSEPCharacter(c)) {
                writer.write("&#");
                writer.write(Integer.toString(c));
                writer.write(59);
            } else if ((!escapingNotNeeded(c) || ((z && this.m_charInfo.shouldMapTextChar(c)) || (!z && this.m_charInfo.shouldMapAttrChar(c)))) && this.m_elemContext.m_currentElemDepth > 0) {
                writer.write("&#");
                writer.write(Integer.toString(c));
                writer.write(59);
            } else {
                writer.write(c);
            }
            return iAccumDefaultEntity + 1;
        }
        return iAccumDefaultEntity;
    }

    @Override
    public void startElement(String str, String str2, String str3, Attributes attributes) throws SAXException {
        if (this.m_inEntityRef) {
            return;
        }
        if (this.m_needToCallStartDocument) {
            startDocumentInternal();
            this.m_needToCallStartDocument = false;
            this.m_docIsEmpty = false;
        } else if (this.m_cdataTagOpen) {
            closeCDATA();
        }
        try {
            if (this.m_needToOutputDocTypeDecl) {
                if (getDoctypeSystem() != null) {
                    outputDocTypeDecl(str3, true);
                }
                this.m_needToOutputDocTypeDecl = false;
            }
            if (this.m_elemContext.m_startTagOpen) {
                closeStartTag();
                this.m_elemContext.m_startTagOpen = false;
            }
            if (str != null) {
                ensurePrefixIsDeclared(str, str3);
            }
            this.m_ispreserve = false;
            if (shouldIndent() && this.m_startNewLine) {
                indent();
            }
            this.m_startNewLine = true;
            Writer writer = this.m_writer;
            writer.write(60);
            writer.write(str3);
            if (attributes != null) {
                addAttributes(attributes);
            }
            this.m_elemContext = this.m_elemContext.push(str, str2, str3);
            this.m_isprevtext = false;
            if (this.m_tracer != null) {
                firePseudoAttributes();
            }
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public void startElement(String str, String str2, String str3) throws SAXException {
        startElement(str, str2, str3, null);
    }

    @Override
    public void startElement(String str) throws SAXException {
        startElement(null, null, str, null);
    }

    void outputDocTypeDecl(String str, boolean z) throws SAXException {
        if (this.m_cdataTagOpen) {
            closeCDATA();
        }
        try {
            Writer writer = this.m_writer;
            writer.write("<!DOCTYPE ");
            writer.write(str);
            String doctypePublic = getDoctypePublic();
            if (doctypePublic != null) {
                writer.write(" PUBLIC \"");
                writer.write(doctypePublic);
                writer.write(34);
            }
            String doctypeSystem = getDoctypeSystem();
            if (doctypeSystem != null) {
                if (doctypePublic == null) {
                    writer.write(" SYSTEM \"");
                } else {
                    writer.write(" \"");
                }
                writer.write(doctypeSystem);
                if (z) {
                    writer.write("\">");
                    writer.write(this.m_lineSep, 0, this.m_lineSepLen);
                } else {
                    writer.write(34);
                }
            }
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    public void processAttributes(Writer writer, int i) throws SAXException, IOException {
        String encoding = getEncoding();
        for (int i2 = 0; i2 < i; i2++) {
            String qName = this.m_attributes.getQName(i2);
            String value = this.m_attributes.getValue(i2);
            writer.write(32);
            writer.write(qName);
            writer.write("=\"");
            writeAttrString(writer, value, encoding);
            writer.write(34);
        }
    }

    public void writeAttrString(Writer writer, String str, String str2) throws IOException {
        int length = str.length();
        if (length > this.m_attrBuff.length) {
            this.m_attrBuff = new char[(length * 2) + 1];
        }
        str.getChars(0, length, this.m_attrBuff, 0);
        char[] cArr = this.m_attrBuff;
        for (int i = 0; i < length; i++) {
            char c = cArr[i];
            if (this.m_charInfo.shouldMapAttrChar(c)) {
                accumDefaultEscape(writer, c, i, cArr, length, false, true);
            } else if (c >= 0 && c <= 31) {
                if (c != '\r') {
                    switch (c) {
                        case '\t':
                            writer.write("&#9;");
                            break;
                        case '\n':
                            writer.write("&#10;");
                            break;
                        default:
                            writer.write("&#");
                            writer.write(Integer.toString(c));
                            writer.write(59);
                            break;
                    }
                } else {
                    writer.write("&#13;");
                }
            } else if (c < 127) {
                writer.write(c);
            } else if (c <= 159) {
                writer.write("&#");
                writer.write(Integer.toString(c));
                writer.write(59);
            } else if (c == 8232) {
                writer.write("&#8232;");
            } else if (this.m_encodingInfo.isInEncoding(c)) {
                writer.write(c);
            } else {
                writer.write("&#");
                writer.write(Integer.toString(c));
                writer.write(59);
            }
        }
    }

    @Override
    public void endElement(String str, String str2, String str3) throws SAXException {
        if (this.m_inEntityRef) {
            return;
        }
        this.m_prefixMap.popNamespaces(this.m_elemContext.m_currentElemDepth, null);
        try {
            Writer writer = this.m_writer;
            if (this.m_elemContext.m_startTagOpen) {
                if (this.m_tracer != null) {
                    super.fireStartElem(this.m_elemContext.m_elementName);
                }
                int length = this.m_attributes.getLength();
                if (length > 0) {
                    processAttributes(this.m_writer, length);
                    this.m_attributes.clear();
                }
                if (this.m_spaceBeforeClose) {
                    writer.write(" />");
                } else {
                    writer.write("/>");
                }
            } else {
                if (this.m_cdataTagOpen) {
                    closeCDATA();
                }
                if (shouldIndent()) {
                    indent(this.m_elemContext.m_currentElemDepth - 1);
                }
                writer.write(60);
                writer.write(47);
                writer.write(str3);
                writer.write(62);
            }
            if (!this.m_elemContext.m_startTagOpen && this.m_doIndent) {
                this.m_ispreserve = this.m_preserves.isEmpty() ? false : this.m_preserves.pop();
            }
            this.m_isprevtext = false;
            if (this.m_tracer != null) {
                super.fireEndElem(str3);
            }
            this.m_elemContext = this.m_elemContext.m_prev;
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public void endElement(String str) throws SAXException {
        endElement(null, null, str);
    }

    @Override
    public void startPrefixMapping(String str, String str2) throws SAXException {
        startPrefixMapping(str, str2, true);
    }

    @Override
    public boolean startPrefixMapping(String str, String str2, boolean z) throws SAXException {
        int i;
        if (z) {
            flushPending();
            i = this.m_elemContext.m_currentElemDepth + 1;
        } else {
            i = this.m_elemContext.m_currentElemDepth;
        }
        boolean zPushNamespace = this.m_prefixMap.pushNamespace(str, str2, i);
        if (zPushNamespace) {
            if ("".equals(str)) {
                addAttributeAlways(SerializerConstants.XMLNS_URI, "xmlns", "xmlns", "CDATA", str2, false);
            } else if (!"".equals(str2)) {
                addAttributeAlways(SerializerConstants.XMLNS_URI, str, Constants.ATTRNAME_XMLNS + str, "CDATA", str2, false);
            }
        }
        return zPushNamespace;
    }

    public void comment(char[] cArr, int i, int i2) throws SAXException {
        if (this.m_inEntityRef) {
            return;
        }
        if (this.m_elemContext.m_startTagOpen) {
            closeStartTag();
            this.m_elemContext.m_startTagOpen = false;
        } else if (this.m_needToCallStartDocument) {
            startDocumentInternal();
            this.m_needToCallStartDocument = false;
        }
        int i3 = i + i2;
        try {
            if (this.m_cdataTagOpen) {
                closeCDATA();
            }
            if (shouldIndent()) {
                indent();
            }
            Writer writer = this.m_writer;
            writer.write(COMMENT_BEGIN);
            int i4 = i;
            int i5 = i4;
            boolean z = false;
            while (i4 < i3) {
                if (z && cArr[i4] == '-') {
                    writer.write(cArr, i5, i4 - i5);
                    writer.write(" -");
                    i5 = i4 + 1;
                }
                z = cArr[i4] == '-';
                i4++;
            }
            if (i2 > 0) {
                int i6 = i3 - i5;
                if (i6 > 0) {
                    writer.write(cArr, i5, i6);
                }
                if (cArr[i3 - 1] == '-') {
                    writer.write(32);
                }
            }
            writer.write(COMMENT_END);
            this.m_startNewLine = true;
            if (this.m_tracer != null) {
                super.fireCommentEvent(cArr, i, i2);
            }
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public void endCDATA() throws SAXException {
        if (this.m_cdataTagOpen) {
            closeCDATA();
        }
        this.m_cdataStartCalled = false;
    }

    public void endDTD() throws SAXException {
        try {
            if (this.m_needToOutputDocTypeDecl) {
                outputDocTypeDecl(this.m_elemContext.m_elementName, false);
                this.m_needToOutputDocTypeDecl = false;
            }
            Writer writer = this.m_writer;
            if (!this.m_inDoctype) {
                writer.write("]>");
            } else {
                writer.write(62);
            }
            writer.write(this.m_lineSep, 0, this.m_lineSepLen);
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public void endPrefixMapping(String str) throws SAXException {
    }

    @Override
    public void ignorableWhitespace(char[] cArr, int i, int i2) throws SAXException {
        if (i2 == 0) {
            return;
        }
        characters(cArr, i, i2);
    }

    @Override
    public void skippedEntity(String str) throws SAXException {
    }

    @Override
    public void startCDATA() throws SAXException {
        this.m_cdataStartCalled = true;
    }

    @Override
    public void startEntity(String str) throws SAXException {
        if (str.equals("[dtd]")) {
            this.m_inExternalDTD = true;
        }
        if (!this.m_expandDTDEntities && !this.m_inExternalDTD) {
            startNonEscaping();
            characters("&" + str + ';');
            endNonEscaping();
        }
        this.m_inEntityRef = true;
    }

    protected void closeStartTag() throws SAXException {
        if (this.m_elemContext.m_startTagOpen) {
            try {
                if (this.m_tracer != null) {
                    super.fireStartElem(this.m_elemContext.m_elementName);
                }
                int length = this.m_attributes.getLength();
                if (length > 0) {
                    processAttributes(this.m_writer, length);
                    this.m_attributes.clear();
                }
                this.m_writer.write(62);
                if (this.m_CdataElems != null) {
                    this.m_elemContext.m_isCdataSection = isCdataSection();
                }
                if (this.m_doIndent) {
                    this.m_isprevtext = false;
                    this.m_preserves.push(this.m_ispreserve);
                }
            } catch (IOException e) {
                throw new SAXException(e);
            }
        }
    }

    public void startDTD(String str, String str2, String str3) throws SAXException {
        setDoctypeSystem(str3);
        setDoctypePublic(str2);
        this.m_elemContext.m_elementName = str;
        this.m_inDoctype = true;
    }

    @Override
    public int getIndentAmount() {
        return this.m_indentAmount;
    }

    @Override
    public void setIndentAmount(int i) {
        this.m_indentAmount = i;
    }

    protected boolean shouldIndent() {
        return this.m_doIndent && !this.m_ispreserve && !this.m_isprevtext && this.m_elemContext.m_currentElemDepth > 0;
    }

    private void setCdataSectionElements(String str, Properties properties) {
        String property = properties.getProperty(str);
        if (property != null) {
            Vector vector = new Vector();
            int length = property.length();
            StringBuffer stringBuffer = new StringBuffer();
            boolean z = false;
            for (int i = 0; i < length; i++) {
                char cCharAt = property.charAt(i);
                if (Character.isWhitespace(cCharAt)) {
                    if (!z) {
                        if (stringBuffer.length() > 0) {
                            addCdataSectionElement(stringBuffer.toString(), vector);
                            stringBuffer.setLength(0);
                        }
                    }
                } else if ('{' != cCharAt) {
                    if ('}' == cCharAt) {
                        z = false;
                    }
                } else {
                    z = true;
                }
                stringBuffer.append(cCharAt);
            }
            if (stringBuffer.length() > 0) {
                addCdataSectionElement(stringBuffer.toString(), vector);
                stringBuffer.setLength(0);
            }
            setCdataSectionElements(vector);
        }
    }

    private void addCdataSectionElement(String str, Vector vector) {
        StringTokenizer stringTokenizer = new StringTokenizer(str, "{}", false);
        String strNextToken = stringTokenizer.nextToken();
        String strNextToken2 = stringTokenizer.hasMoreTokens() ? stringTokenizer.nextToken() : null;
        if (strNextToken2 == null) {
            vector.addElement(null);
            vector.addElement(strNextToken);
        } else {
            vector.addElement(strNextToken);
            vector.addElement(strNextToken2);
        }
    }

    @Override
    public void setCdataSectionElements(Vector vector) {
        int size;
        if (vector != null && vector.size() - 1 > 0) {
            StringBuffer stringBuffer = new StringBuffer();
            for (int i = 0; i < size; i += 2) {
                if (i != 0) {
                    stringBuffer.append(' ');
                }
                String str = (String) vector.elementAt(i);
                String str2 = (String) vector.elementAt(i + 1);
                if (str != null) {
                    stringBuffer.append('{');
                    stringBuffer.append(str);
                    stringBuffer.append('}');
                }
                stringBuffer.append(str2);
            }
            this.m_StringOfCDATASections = stringBuffer.toString();
        }
        initCdataElems(this.m_StringOfCDATASections);
    }

    protected String ensureAttributesNamespaceIsDeclared(String str, String str2, String str3) throws SAXException {
        String strSubstring;
        if (str == null || str.length() <= 0) {
            return null;
        }
        int iIndexOf = str3.indexOf(":");
        if (iIndexOf < 0) {
            strSubstring = "";
        } else {
            strSubstring = str3.substring(0, iIndexOf);
        }
        if (iIndexOf > 0) {
            String strLookupNamespace = this.m_prefixMap.lookupNamespace(strSubstring);
            if (strLookupNamespace != null && strLookupNamespace.equals(str)) {
                return null;
            }
            startPrefixMapping(strSubstring, str, false);
            addAttribute(SerializerConstants.XMLNS_URI, strSubstring, Constants.ATTRNAME_XMLNS + strSubstring, "CDATA", str, false);
            return strSubstring;
        }
        String strLookupPrefix = this.m_prefixMap.lookupPrefix(str);
        if (strLookupPrefix == null) {
            String strGenerateNextPrefix = this.m_prefixMap.generateNextPrefix();
            startPrefixMapping(strGenerateNextPrefix, str, false);
            addAttribute(SerializerConstants.XMLNS_URI, strGenerateNextPrefix, Constants.ATTRNAME_XMLNS + strGenerateNextPrefix, "CDATA", str, false);
            return strGenerateNextPrefix;
        }
        return strLookupPrefix;
    }

    void ensurePrefixIsDeclared(String str, String str2) throws SAXException {
        String str3;
        if (str != null && str.length() > 0) {
            int iIndexOf = str2.indexOf(":");
            boolean z = iIndexOf < 0;
            String strSubstring = z ? "" : str2.substring(0, iIndexOf);
            if (strSubstring != null) {
                String strLookupNamespace = this.m_prefixMap.lookupNamespace(strSubstring);
                if (strLookupNamespace == null || !strLookupNamespace.equals(str)) {
                    startPrefixMapping(strSubstring, str);
                    String str4 = z ? "xmlns" : strSubstring;
                    if (z) {
                        str3 = "xmlns";
                    } else {
                        str3 = Constants.ATTRNAME_XMLNS + strSubstring;
                    }
                    addAttributeAlways(SerializerConstants.XMLNS_URI, str4, str3, "CDATA", str, false);
                }
            }
        }
    }

    @Override
    public void flushPending() throws SAXException {
        if (this.m_needToCallStartDocument) {
            startDocumentInternal();
            this.m_needToCallStartDocument = false;
        }
        if (this.m_elemContext.m_startTagOpen) {
            closeStartTag();
            this.m_elemContext.m_startTagOpen = false;
        }
        if (this.m_cdataTagOpen) {
            closeCDATA();
            this.m_cdataTagOpen = false;
        }
        if (this.m_writer != null) {
            try {
                this.m_writer.flush();
            } catch (IOException e) {
            }
        }
    }

    @Override
    public void setContentHandler(ContentHandler contentHandler) {
    }

    @Override
    public boolean addAttributeAlways(String str, String str2, String str3, String str4, String str5, boolean z) {
        int index;
        if (str == null || str2 == null || str.length() == 0) {
            index = this.m_attributes.getIndex(str3);
        } else {
            index = this.m_attributes.getIndex(str, str2);
        }
        boolean z2 = false;
        if (index >= 0) {
            String str6 = null;
            if (this.m_tracer != null) {
                String value = this.m_attributes.getValue(index);
                if (!str5.equals(value)) {
                    str6 = value;
                }
            }
            this.m_attributes.setValue(index, str5);
            if (str6 != null) {
                firePseudoAttributes();
            }
        } else {
            if (z) {
                int iIndexOf = str3.indexOf(58);
                if (iIndexOf > 0) {
                    NamespaceMappings.MappingRecord mappingFromPrefix = this.m_prefixMap.getMappingFromPrefix(str3.substring(0, iIndexOf));
                    if (mappingFromPrefix != null && mappingFromPrefix.m_declarationDepth == this.m_elemContext.m_currentElemDepth && !mappingFromPrefix.m_uri.equals(str)) {
                        String strLookupPrefix = this.m_prefixMap.lookupPrefix(str);
                        if (strLookupPrefix == null) {
                            strLookupPrefix = this.m_prefixMap.generateNextPrefix();
                        }
                        str3 = strLookupPrefix + ':' + str2;
                    }
                }
                try {
                    ensureAttributesNamespaceIsDeclared(str, str2, str3);
                } catch (SAXException e) {
                    e.printStackTrace();
                }
            }
            this.m_attributes.addAttribute(str, str2, str3, str4, str5);
            z2 = true;
            if (this.m_tracer != null) {
                firePseudoAttributes();
            }
        }
        return z2;
    }

    protected void firePseudoAttributes() {
        if (this.m_tracer != null) {
            try {
                this.m_writer.flush();
                StringBuffer stringBuffer = new StringBuffer();
                int length = this.m_attributes.getLength();
                if (length > 0) {
                    processAttributes(new WritertoStringBuffer(stringBuffer), length);
                }
                stringBuffer.append('>');
                char[] charArray = stringBuffer.toString().toCharArray();
                this.m_tracer.fireGenerateEvent(11, charArray, 0, charArray.length);
            } catch (IOException e) {
            } catch (SAXException e2) {
            }
        }
    }

    private class WritertoStringBuffer extends Writer {
        private final StringBuffer m_stringbuf;

        WritertoStringBuffer(StringBuffer stringBuffer) {
            this.m_stringbuf = stringBuffer;
        }

        @Override
        public void write(char[] cArr, int i, int i2) throws IOException {
            this.m_stringbuf.append(cArr, i, i2);
        }

        @Override
        public void flush() throws IOException {
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public void write(int i) {
            this.m_stringbuf.append((char) i);
        }

        @Override
        public void write(String str) {
            this.m_stringbuf.append(str);
        }
    }

    @Override
    public void setTransformer(Transformer transformer) {
        super.setTransformer(transformer);
        if (this.m_tracer != null && !(this.m_writer instanceof SerializerTraceWriter)) {
            setWriterInternal(new SerializerTraceWriter(this.m_writer, this.m_tracer), false);
        }
    }

    @Override
    public boolean reset() {
        if (super.reset()) {
            resetToStream();
            return true;
        }
        return false;
    }

    private void resetToStream() {
        this.m_cdataStartCalled = false;
        this.m_disableOutputEscapingStates.clear();
        this.m_escaping = true;
        this.m_expandDTDEntities = true;
        this.m_inDoctype = false;
        this.m_ispreserve = false;
        this.m_isprevtext = false;
        this.m_isUTF8 = false;
        this.m_lineSep = s_systemLineSep;
        this.m_lineSepLen = s_systemLineSep.length;
        this.m_lineSepUse = true;
        this.m_preserves.clear();
        this.m_shouldFlush = true;
        this.m_spaceBeforeClose = false;
        this.m_startNewLine = false;
        this.m_writer_set_by_user = false;
    }

    @Override
    public void setEncoding(String str) {
        setOutputProperty("encoding", str);
    }

    static final class BoolStack {
        private int m_allocatedSize;
        private int m_index;
        private boolean[] m_values;

        public BoolStack() {
            this(32);
        }

        public BoolStack(int i) {
            this.m_allocatedSize = i;
            this.m_values = new boolean[i];
            this.m_index = -1;
        }

        public final int size() {
            return this.m_index + 1;
        }

        public final void clear() {
            this.m_index = -1;
        }

        public final boolean push(boolean z) {
            if (this.m_index == this.m_allocatedSize - 1) {
                grow();
            }
            boolean[] zArr = this.m_values;
            int i = this.m_index + 1;
            this.m_index = i;
            zArr[i] = z;
            return z;
        }

        public final boolean pop() {
            boolean[] zArr = this.m_values;
            int i = this.m_index;
            this.m_index = i - 1;
            return zArr[i];
        }

        public final boolean popAndTop() {
            this.m_index--;
            if (this.m_index >= 0) {
                return this.m_values[this.m_index];
            }
            return false;
        }

        public final void setTop(boolean z) {
            this.m_values[this.m_index] = z;
        }

        public final boolean peek() {
            return this.m_values[this.m_index];
        }

        public final boolean peekOrFalse() {
            if (this.m_index > -1) {
                return this.m_values[this.m_index];
            }
            return false;
        }

        public final boolean peekOrTrue() {
            if (this.m_index > -1) {
                return this.m_values[this.m_index];
            }
            return true;
        }

        public boolean isEmpty() {
            return this.m_index == -1;
        }

        private void grow() {
            this.m_allocatedSize *= 2;
            boolean[] zArr = new boolean[this.m_allocatedSize];
            System.arraycopy(this.m_values, 0, zArr, 0, this.m_index + 1);
            this.m_values = zArr;
        }
    }

    @Override
    public void notationDecl(String str, String str2, String str3) throws SAXException {
        try {
            DTDprolog();
            this.m_writer.write("<!NOTATION ");
            this.m_writer.write(str);
            if (str2 != null) {
                this.m_writer.write(" PUBLIC \"");
                this.m_writer.write(str2);
            } else {
                this.m_writer.write(" SYSTEM \"");
                this.m_writer.write(str3);
            }
            this.m_writer.write("\" >");
            this.m_writer.write(this.m_lineSep, 0, this.m_lineSepLen);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void unparsedEntityDecl(String str, String str2, String str3, String str4) throws SAXException {
        try {
            DTDprolog();
            this.m_writer.write("<!ENTITY ");
            this.m_writer.write(str);
            if (str2 != null) {
                this.m_writer.write(" PUBLIC \"");
                this.m_writer.write(str2);
            } else {
                this.m_writer.write(" SYSTEM \"");
                this.m_writer.write(str3);
            }
            this.m_writer.write("\" NDATA ");
            this.m_writer.write(str4);
            this.m_writer.write(" >");
            this.m_writer.write(this.m_lineSep, 0, this.m_lineSepLen);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void DTDprolog() throws SAXException, IOException {
        Writer writer = this.m_writer;
        if (this.m_needToOutputDocTypeDecl) {
            outputDocTypeDecl(this.m_elemContext.m_elementName, false);
            this.m_needToOutputDocTypeDecl = false;
        }
        if (this.m_inDoctype) {
            writer.write(" [");
            writer.write(this.m_lineSep, 0, this.m_lineSepLen);
            this.m_inDoctype = false;
        }
    }

    @Override
    public void setDTDEntityExpansion(boolean z) {
        this.m_expandDTDEntities = z;
    }

    public void setNewLine(char[] cArr) {
        this.m_lineSep = cArr;
        this.m_lineSepLen = cArr.length;
    }

    public void addCdataSectionElements(String str) {
        if (str != null) {
            initCdataElems(str);
        }
        if (this.m_StringOfCDATASections == null) {
            this.m_StringOfCDATASections = str;
            return;
        }
        this.m_StringOfCDATASections += " " + str;
    }
}
