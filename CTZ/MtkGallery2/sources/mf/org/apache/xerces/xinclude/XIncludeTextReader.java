package mf.org.apache.xerces.xinclude;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import mf.org.apache.xerces.impl.XMLEntityManager;
import mf.org.apache.xerces.impl.XMLErrorReporter;
import mf.org.apache.xerces.impl.io.ASCIIReader;
import mf.org.apache.xerces.impl.io.Latin1Reader;
import mf.org.apache.xerces.impl.io.UTF16Reader;
import mf.org.apache.xerces.impl.io.UTF8Reader;
import mf.org.apache.xerces.util.EncodingMap;
import mf.org.apache.xerces.util.HTTPInputSource;
import mf.org.apache.xerces.util.MessageFormatter;
import mf.org.apache.xerces.util.XMLChar;
import mf.org.apache.xerces.xni.XMLString;
import mf.org.apache.xerces.xni.parser.XMLInputSource;

public class XIncludeTextReader {
    private XMLErrorReporter fErrorReporter;
    private final XIncludeHandler fHandler;
    private Reader fReader;
    private XMLInputSource fSource;
    private XMLString fTempString;

    public XIncludeTextReader(XMLInputSource source, XIncludeHandler handler, int bufferSize) throws IOException {
        this.fTempString = new XMLString();
        this.fHandler = handler;
        this.fSource = source;
        this.fTempString = new XMLString(new char[bufferSize + 1], 0, 0);
    }

    public void setErrorReporter(XMLErrorReporter errorReporter) {
        this.fErrorReporter = errorReporter;
    }

    protected Reader getReader(XMLInputSource xMLInputSource) throws IOException {
        InputStream stream;
        String contentType;
        if (xMLInputSource.getCharacterStream() != null) {
            return xMLInputSource.getCharacterStream();
        }
        String encoding = xMLInputSource.getEncoding();
        if (encoding == null) {
            encoding = "UTF-8";
        }
        if (xMLInputSource.getByteStream() != null) {
            stream = xMLInputSource.getByteStream();
            if (!(stream instanceof BufferedInputStream)) {
                stream = new BufferedInputStream(stream, this.fTempString.ch.length);
            }
        } else {
            String expandedSystemId = XMLEntityManager.expandSystemId(xMLInputSource.getSystemId(), xMLInputSource.getBaseSystemId(), false);
            URL url = new URL(expandedSystemId);
            ?? OpenConnection = url.openConnection();
            if ((OpenConnection instanceof HttpURLConnection) && (xMLInputSource instanceof HTTPInputSource)) {
                Iterator propIter = xMLInputSource.getHTTPRequestProperties();
                while (propIter.hasNext()) {
                    Map.Entry entry = (Map.Entry) propIter.next();
                    OpenConnection.setRequestProperty((String) entry.getKey(), (String) entry.getValue());
                }
                boolean followRedirects = xMLInputSource.getFollowHTTPRedirects();
                if (!followRedirects) {
                    OpenConnection.setInstanceFollowRedirects(followRedirects);
                }
            }
            stream = new BufferedInputStream(OpenConnection.getInputStream());
            String rawContentType = OpenConnection.getContentType();
            int index = rawContentType != null ? rawContentType.indexOf(59) : -1;
            String charset = null;
            if (index != -1) {
                contentType = rawContentType.substring(0, index).trim();
                String charset2 = rawContentType.substring(index + 1).trim();
                if (charset2.startsWith("charset=")) {
                    charset = charset2.substring(8).trim();
                    if ((charset.charAt(0) == '\"' && charset.charAt(charset.length() - 1) == '\"') || (charset.charAt(0) == '\'' && charset.charAt(charset.length() - 1) == '\'')) {
                        charset = charset.substring(1, charset.length() - 1);
                    }
                } else {
                    charset = null;
                }
            } else {
                contentType = rawContentType.trim();
            }
            String detectedEncoding = null;
            if (contentType.equals("text/xml")) {
                if (charset != null) {
                    detectedEncoding = charset;
                } else {
                    detectedEncoding = "US-ASCII";
                }
            } else if (contentType.equals("application/xml")) {
                if (charset != null) {
                    detectedEncoding = charset;
                } else {
                    detectedEncoding = getEncodingName(stream);
                }
            } else if (contentType.endsWith("+xml")) {
                detectedEncoding = getEncodingName(stream);
            }
            if (detectedEncoding != null) {
                encoding = detectedEncoding;
            }
        }
        String encoding2 = consumeBOM(stream, encoding.toUpperCase(Locale.ENGLISH));
        if (encoding2.equals("UTF-8")) {
            return createUTF8Reader(stream);
        }
        if (encoding2.equals("UTF-16BE")) {
            return createUTF16Reader(stream, true);
        }
        if (encoding2.equals("UTF-16LE")) {
            return createUTF16Reader(stream, false);
        }
        String javaEncoding = EncodingMap.getIANA2JavaMapping(encoding2);
        if (javaEncoding == null) {
            MessageFormatter aFormatter = this.fErrorReporter.getMessageFormatter("http://www.w3.org/TR/1998/REC-xml-19980210");
            Locale aLocale = this.fErrorReporter.getLocale();
            throw new IOException(aFormatter.formatMessage(aLocale, "EncodingDeclInvalid", new Object[]{encoding2}));
        }
        if (javaEncoding.equals("ASCII")) {
            return createASCIIReader(stream);
        }
        if (javaEncoding.equals("ISO8859_1")) {
            return createLatin1Reader(stream);
        }
        return new InputStreamReader(stream, javaEncoding);
    }

    private Reader createUTF8Reader(InputStream stream) {
        return new UTF8Reader(stream, this.fTempString.ch.length, this.fErrorReporter.getMessageFormatter("http://www.w3.org/TR/1998/REC-xml-19980210"), this.fErrorReporter.getLocale());
    }

    private Reader createUTF16Reader(InputStream stream, boolean isBigEndian) {
        return new UTF16Reader(stream, this.fTempString.ch.length << 1, isBigEndian, this.fErrorReporter.getMessageFormatter("http://www.w3.org/TR/1998/REC-xml-19980210"), this.fErrorReporter.getLocale());
    }

    private Reader createASCIIReader(InputStream stream) {
        return new ASCIIReader(stream, this.fTempString.ch.length, this.fErrorReporter.getMessageFormatter("http://www.w3.org/TR/1998/REC-xml-19980210"), this.fErrorReporter.getLocale());
    }

    private Reader createLatin1Reader(InputStream stream) {
        return new Latin1Reader(stream, this.fTempString.ch.length);
    }

    protected String getEncodingName(InputStream stream) throws IOException {
        byte[] b4 = new byte[4];
        stream.mark(4);
        int count = stream.read(b4, 0, 4);
        stream.reset();
        if (count != 4) {
            return null;
        }
        String encoding = getEncodingName(b4);
        return encoding;
    }

    protected String consumeBOM(InputStream stream, String encoding) throws IOException {
        byte[] b = new byte[3];
        stream.mark(3);
        if (encoding.equals("UTF-8")) {
            int count = stream.read(b, 0, 3);
            if (count == 3) {
                int b0 = b[0] & 255;
                int b1 = b[1] & 255;
                int b2 = b[2] & 255;
                if (b0 != 239 || b1 != 187 || b2 != 191) {
                    stream.reset();
                }
            } else {
                stream.reset();
            }
        } else if (encoding.startsWith("UTF-16")) {
            int count2 = stream.read(b, 0, 2);
            if (count2 == 2) {
                int b02 = b[0] & 255;
                int b12 = b[1] & 255;
                if (b02 == 254 && b12 == 255) {
                    return "UTF-16BE";
                }
                if (b02 == 255 && b12 == 254) {
                    return "UTF-16LE";
                }
            }
            stream.reset();
        }
        return encoding;
    }

    protected String getEncodingName(byte[] b4) {
        int b0 = b4[0] & 255;
        int b1 = b4[1] & 255;
        if (b0 == 254 && b1 == 255) {
            return "UTF-16BE";
        }
        if (b0 == 255 && b1 == 254) {
            return "UTF-16LE";
        }
        int b2 = b4[2] & 255;
        if (b0 == 239 && b1 == 187 && b2 == 191) {
            return "UTF-8";
        }
        int b3 = 255 & b4[3];
        if (b0 == 0 && b1 == 0 && b2 == 0 && b3 == 60) {
            return "ISO-10646-UCS-4";
        }
        if (b0 == 60 && b1 == 0 && b2 == 0 && b3 == 0) {
            return "ISO-10646-UCS-4";
        }
        if (b0 == 0 && b1 == 0 && b2 == 60 && b3 == 0) {
            return "ISO-10646-UCS-4";
        }
        if (b0 != 0 || b1 != 60 || b2 != 0 || b3 != 0) {
            if (b0 == 0 && b1 == 60 && b2 == 0 && b3 == 63) {
                return "UTF-16BE";
            }
            if (b0 == 60 && b1 == 0 && b2 == 63 && b3 == 0) {
                return "UTF-16LE";
            }
            if (b0 == 76 && b1 == 111 && b2 == 167 && b3 == 148) {
                return "CP037";
            }
            return null;
        }
        return "ISO-10646-UCS-4";
    }

    public void parse() throws IOException {
        int i;
        this.fReader = getReader(this.fSource);
        this.fSource = null;
        int i2 = this.fReader.read(this.fTempString.ch, 0, this.fTempString.ch.length - 1);
        this.fHandler.fHasIncludeReportedContent = true;
        while (i2 != -1) {
            int i3 = 0;
            while (i3 < i2) {
                char c = this.fTempString.ch[i3];
                if (!isValid(c)) {
                    if (XMLChar.isHighSurrogate(c)) {
                        i3++;
                        if (i3 < i2) {
                            i = this.fTempString.ch[i3];
                        } else {
                            int i4 = this.fReader.read();
                            i = i4;
                            if (i4 != -1) {
                                this.fTempString.ch[i2] = (char) i4;
                                i2++;
                                i = i4;
                            }
                        }
                        if (XMLChar.isLowSurrogate(i)) {
                            int iSupplemental = XMLChar.supplemental(c, (char) i);
                            if (!isValid(iSupplemental)) {
                                this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "InvalidCharInContent", new Object[]{Integer.toString(iSupplemental, 16)}, (short) 2);
                            }
                        } else {
                            this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "InvalidCharInContent", new Object[]{Integer.toString(i, 16)}, (short) 2);
                        }
                    } else {
                        this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "InvalidCharInContent", new Object[]{Integer.toString(c, 16)}, (short) 2);
                    }
                }
                i3++;
            }
            if (this.fHandler != null && i2 > 0) {
                this.fTempString.offset = 0;
                this.fTempString.length = i2;
                this.fHandler.characters(this.fTempString, this.fHandler.modifyAugmentations(null, true));
            }
            i2 = this.fReader.read(this.fTempString.ch, 0, this.fTempString.ch.length - 1);
        }
    }

    public void setInputSource(XMLInputSource source) {
        this.fSource = source;
    }

    public void close() throws IOException {
        if (this.fReader != null) {
            this.fReader.close();
            this.fReader = null;
        }
    }

    protected boolean isValid(int ch) {
        return XMLChar.isValid(ch);
    }

    protected void setBufferSize(int bufferSize) {
        int bufferSize2 = bufferSize + 1;
        if (this.fTempString.ch.length != bufferSize2) {
            this.fTempString.ch = new char[bufferSize2];
        }
    }
}
