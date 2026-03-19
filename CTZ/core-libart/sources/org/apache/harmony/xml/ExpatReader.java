package org.apache.harmony.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import libcore.io.IoUtils;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

public class ExpatReader implements XMLReader {
    private static final String LEXICAL_HANDLER_PROPERTY = "http://xml.org/sax/properties/lexical-handler";
    ContentHandler contentHandler;
    DTDHandler dtdHandler;
    EntityResolver entityResolver;
    ErrorHandler errorHandler;
    LexicalHandler lexicalHandler;
    private boolean processNamespaces = true;
    private boolean processNamespacePrefixes = false;

    private static class Feature {
        private static final String BASE_URI = "http://xml.org/sax/features/";
        private static final String EXTERNAL_GENERAL_ENTITIES = "http://xml.org/sax/features/external-general-entities";
        private static final String EXTERNAL_PARAMETER_ENTITIES = "http://xml.org/sax/features/external-parameter-entities";
        private static final String NAMESPACES = "http://xml.org/sax/features/namespaces";
        private static final String NAMESPACE_PREFIXES = "http://xml.org/sax/features/namespace-prefixes";
        private static final String STRING_INTERNING = "http://xml.org/sax/features/string-interning";
        private static final String VALIDATION = "http://xml.org/sax/features/validation";

        private Feature() {
        }
    }

    @Override
    public boolean getFeature(String str) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (str == null) {
            throw new NullPointerException("name == null");
        }
        if (str.equals("http://xml.org/sax/features/validation") || str.equals("http://xml.org/sax/features/external-general-entities") || str.equals("http://xml.org/sax/features/external-parameter-entities")) {
            return false;
        }
        if (str.equals("http://xml.org/sax/features/namespaces")) {
            return this.processNamespaces;
        }
        if (str.equals("http://xml.org/sax/features/namespace-prefixes")) {
            return this.processNamespacePrefixes;
        }
        if (str.equals("http://xml.org/sax/features/string-interning")) {
            return true;
        }
        throw new SAXNotRecognizedException(str);
    }

    @Override
    public void setFeature(String str, boolean z) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (str == null) {
            throw new NullPointerException("name == null");
        }
        if (str.equals("http://xml.org/sax/features/validation") || str.equals("http://xml.org/sax/features/external-general-entities") || str.equals("http://xml.org/sax/features/external-parameter-entities")) {
            if (z) {
                throw new SAXNotSupportedException("Cannot enable " + str);
            }
            return;
        }
        if (str.equals("http://xml.org/sax/features/namespaces")) {
            this.processNamespaces = z;
            return;
        }
        if (str.equals("http://xml.org/sax/features/namespace-prefixes")) {
            this.processNamespacePrefixes = z;
            return;
        }
        if (str.equals("http://xml.org/sax/features/string-interning")) {
            if (z) {
                return;
            }
            throw new SAXNotSupportedException("Cannot disable " + str);
        }
        throw new SAXNotRecognizedException(str);
    }

    @Override
    public Object getProperty(String str) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (str == null) {
            throw new NullPointerException("name == null");
        }
        if (str.equals(LEXICAL_HANDLER_PROPERTY)) {
            return this.lexicalHandler;
        }
        throw new SAXNotRecognizedException(str);
    }

    @Override
    public void setProperty(String str, Object obj) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (str == null) {
            throw new NullPointerException("name == null");
        }
        if (str.equals(LEXICAL_HANDLER_PROPERTY)) {
            if ((obj instanceof LexicalHandler) || obj == null) {
                this.lexicalHandler = (LexicalHandler) obj;
                return;
            }
            throw new SAXNotSupportedException("value doesn't implement org.xml.sax.ext.LexicalHandler");
        }
        throw new SAXNotRecognizedException(str);
    }

    @Override
    public void setEntityResolver(EntityResolver entityResolver) {
        this.entityResolver = entityResolver;
    }

    @Override
    public EntityResolver getEntityResolver() {
        return this.entityResolver;
    }

    @Override
    public void setDTDHandler(DTDHandler dTDHandler) {
        this.dtdHandler = dTDHandler;
    }

    @Override
    public DTDHandler getDTDHandler() {
        return this.dtdHandler;
    }

    @Override
    public void setContentHandler(ContentHandler contentHandler) {
        this.contentHandler = contentHandler;
    }

    @Override
    public ContentHandler getContentHandler() {
        return this.contentHandler;
    }

    @Override
    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    @Override
    public ErrorHandler getErrorHandler() {
        return this.errorHandler;
    }

    public LexicalHandler getLexicalHandler() {
        return this.lexicalHandler;
    }

    public void setLexicalHandler(LexicalHandler lexicalHandler) {
        this.lexicalHandler = lexicalHandler;
    }

    public boolean isNamespaceProcessingEnabled() {
        return this.processNamespaces;
    }

    public void setNamespaceProcessingEnabled(boolean z) {
        this.processNamespaces = z;
    }

    @Override
    public void parse(InputSource inputSource) throws SAXException, IOException {
        if (this.processNamespacePrefixes && this.processNamespaces) {
            throw new SAXNotSupportedException("The 'namespace-prefix' feature is not supported while the 'namespaces' feature is enabled.");
        }
        Reader characterStream = inputSource.getCharacterStream();
        if (characterStream != null) {
            try {
                parse(characterStream, inputSource.getPublicId(), inputSource.getSystemId());
                return;
            } finally {
                IoUtils.closeQuietly(characterStream);
            }
        }
        InputStream byteStream = inputSource.getByteStream();
        String encoding = inputSource.getEncoding();
        if (byteStream != null) {
            try {
                parse(byteStream, encoding, inputSource.getPublicId(), inputSource.getSystemId());
                return;
            } finally {
                IoUtils.closeQuietly(byteStream);
            }
        }
        String systemId = inputSource.getSystemId();
        if (systemId == null) {
            throw new SAXException("No input specified.");
        }
        InputStream inputStreamOpenUrl = ExpatParser.openUrl(systemId);
        try {
            parse(inputStreamOpenUrl, encoding, inputSource.getPublicId(), systemId);
        } finally {
            IoUtils.closeQuietly(inputStreamOpenUrl);
        }
    }

    private void parse(Reader reader, String str, String str2) throws SAXException, IOException {
        new ExpatParser("UTF-16", this, this.processNamespaces, str, str2).parseDocument(reader);
    }

    private void parse(InputStream inputStream, String str, String str2, String str3) throws SAXException, IOException {
        new ExpatParser(str, this, this.processNamespaces, str2, str3).parseDocument(inputStream);
    }

    @Override
    public void parse(String str) throws SAXException, IOException {
        parse(new InputSource(str));
    }
}
