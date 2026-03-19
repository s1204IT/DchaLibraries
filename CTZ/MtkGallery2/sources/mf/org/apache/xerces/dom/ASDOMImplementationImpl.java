package mf.org.apache.xerces.dom;

import mf.org.apache.xerces.dom3.as.ASModel;
import mf.org.apache.xerces.dom3.as.DOMASBuilder;
import mf.org.apache.xerces.dom3.as.DOMASWriter;
import mf.org.apache.xerces.dom3.as.DOMImplementationAS;
import mf.org.apache.xerces.parsers.DOMASBuilderImpl;
import mf.org.w3c.dom.DOMException;
import mf.org.w3c.dom.DOMImplementation;

public class ASDOMImplementationImpl extends DOMImplementationImpl implements DOMImplementationAS {
    static final ASDOMImplementationImpl singleton = new ASDOMImplementationImpl();

    public static DOMImplementation getDOMImplementation() {
        return singleton;
    }

    @Override
    public ASModel createAS(boolean isNamespaceAware) {
        return new ASModelImpl(isNamespaceAware);
    }

    @Override
    public DOMASBuilder createDOMASBuilder() {
        return new DOMASBuilderImpl();
    }

    @Override
    public DOMASWriter createDOMASWriter() {
        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
        throw new DOMException((short) 9, msg);
    }
}
