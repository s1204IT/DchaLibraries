package mf.org.apache.xerces.dom;

import mf.org.w3c.dom.DOMImplementation;
import mf.org.w3c.dom.DocumentType;

public class DOMImplementationImpl extends CoreDOMImplementationImpl implements DOMImplementation {
    static final DOMImplementationImpl singleton = new DOMImplementationImpl();

    public static DOMImplementation getDOMImplementation() {
        return singleton;
    }

    @Override
    public boolean hasFeature(String feature, String version) {
        boolean result = super.hasFeature(feature, version);
        if (!result) {
            boolean anyVersion = version == null || version.length() == 0;
            if (feature.startsWith("+")) {
                feature = feature.substring(1);
            }
            return (feature.equalsIgnoreCase("Events") && (anyVersion || version.equals("2.0"))) || (feature.equalsIgnoreCase("MutationEvents") && (anyVersion || version.equals("2.0"))) || ((feature.equalsIgnoreCase("Traversal") && (anyVersion || version.equals("2.0"))) || ((feature.equalsIgnoreCase("Range") && (anyVersion || version.equals("2.0"))) || (feature.equalsIgnoreCase("MutationEvents") && (anyVersion || version.equals("2.0")))));
        }
        return result;
    }

    @Override
    protected CoreDocumentImpl createDocument(DocumentType doctype) {
        return new DocumentImpl(doctype);
    }
}
