package mf.org.apache.xml.serialize;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import mf.org.apache.xerces.dom.DOMMessageFormatter;

final class SerializerFactoryImpl extends SerializerFactory {
    private String _method;

    SerializerFactoryImpl(String method) {
        this._method = method;
        if (!this._method.equals("xml") && !this._method.equals("html") && !this._method.equals("xhtml") && !this._method.equals("text")) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.SERIALIZER_DOMAIN, "MethodNotSupported", new Object[]{method});
            throw new IllegalArgumentException(msg);
        }
    }

    @Override
    public Serializer makeSerializer(Writer writer, OutputFormat format) {
        Serializer serializer = getSerializer(format);
        serializer.setOutputCharStream(writer);
        return serializer;
    }

    @Override
    public Serializer makeSerializer(OutputStream output, OutputFormat format) throws UnsupportedEncodingException {
        Serializer serializer = getSerializer(format);
        serializer.setOutputByteStream(output);
        return serializer;
    }

    private Serializer getSerializer(OutputFormat format) {
        if (this._method.equals("xml")) {
            return new XMLSerializer(format);
        }
        if (this._method.equals("html")) {
            return new HTMLSerializer(format);
        }
        if (this._method.equals("xhtml")) {
            return new XHTMLSerializer(format);
        }
        if (this._method.equals("text")) {
            return new TextSerializer();
        }
        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.SERIALIZER_DOMAIN, "MethodNotSupported", new Object[]{this._method});
        throw new IllegalStateException(msg);
    }

    @Override
    protected String getSupportedMethod() {
        return this._method;
    }
}
