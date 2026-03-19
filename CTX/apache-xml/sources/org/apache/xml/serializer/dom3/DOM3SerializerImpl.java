package org.apache.xml.serializer.dom3;

import java.io.IOException;
import org.apache.xml.serializer.DOM3Serializer;
import org.apache.xml.serializer.SerializationHandler;
import org.apache.xml.serializer.utils.WrappedRuntimeException;
import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.Node;
import org.w3c.dom.ls.LSSerializerFilter;
import org.xml.sax.SAXException;

public final class DOM3SerializerImpl implements DOM3Serializer {
    private DOMErrorHandler fErrorHandler;
    private String fNewLine;
    private SerializationHandler fSerializationHandler;
    private LSSerializerFilter fSerializerFilter;

    public DOM3SerializerImpl(SerializationHandler serializationHandler) {
        this.fSerializationHandler = serializationHandler;
    }

    @Override
    public DOMErrorHandler getErrorHandler() {
        return this.fErrorHandler;
    }

    @Override
    public LSSerializerFilter getNodeFilter() {
        return this.fSerializerFilter;
    }

    public char[] getNewLine() {
        if (this.fNewLine != null) {
            return this.fNewLine.toCharArray();
        }
        return null;
    }

    @Override
    public void serializeDOM3(Node node) throws IOException {
        try {
            new DOM3TreeWalker(this.fSerializationHandler, this.fErrorHandler, this.fSerializerFilter, this.fNewLine).traverse(node);
        } catch (SAXException e) {
            throw new WrappedRuntimeException(e);
        }
    }

    @Override
    public void setErrorHandler(DOMErrorHandler dOMErrorHandler) {
        this.fErrorHandler = dOMErrorHandler;
    }

    @Override
    public void setNodeFilter(LSSerializerFilter lSSerializerFilter) {
        this.fSerializerFilter = lSSerializerFilter;
    }

    public void setSerializationHandler(SerializationHandler serializationHandler) {
        this.fSerializationHandler = serializationHandler;
    }

    @Override
    public void setNewLine(char[] cArr) {
        this.fNewLine = cArr != null ? new String(cArr) : null;
    }
}
